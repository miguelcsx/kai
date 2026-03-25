package kai.engine

import kai.domain.campaign.CampaignConfig
import kai.domain.campaign.CampaignState
import kai.domain.campaign.CampaignStats
import kai.domain.campaign.CampaignStatus
import kai.domain.finding.Finding
import kai.domain.finding.OracleVerdict
import kai.domain.observations.Observations
import kai.domain.result.KaiResult
import kai.domain.testcase.TestCase
import kai.engine.event.CampaignEvent
import kai.plugin.registry.PluginRegistry
import kai.plugin.registry.ValidationResult
import kai.plugin.strategy.GenerationContext
import kai.storage.api.StoragePorts

class CampaignEngine(
    private val registry: PluginRegistry,
    private val storage: StoragePorts,
    private val verifier: FindingVerifier = OracleFindingVerifier(registry),
    private val reductionService: ReductionService = ReductionService(registry, verifier)
) {
    fun run(
        config: CampaignConfig,
        onEvent: (CampaignEvent) -> Unit
    ): KaiResult<CampaignState> {
        val validation = validate(config)
        if (validation is KaiResult.Failure) {
            return validation
        }
        var state = CampaignState.initial(config)
        return try {
            val reduction = reductionService.forConfig(config)
            saveState(state, onEvent)
            onEvent(CampaignEvent.Started(state))
            val signals = linkedMapOf<kai.domain.id.StrategyId, MutableList<kai.plugin.strategy.FitnessSignal>>()
            while (hasBudget(state)) {
                state = state.copy(status = CampaignStatus.RUNNING)
                val testCase = nextTestCase(state, signals)
                storeTestCase(testCase, onEvent)
                val observations = execute(state, testCase)
                val findings = evaluate(state.config, observations, testCase, reduction, onEvent)
                updateSignals(observations, testCase, signals)
                state = advance(state, findings.size)
                saveState(state, onEvent)
                if (state.stats.totalInteresting >= state.config.budget.maxFindings) {
                    break
                }
            }
            val finished = state.copy(status = CampaignStatus.FINISHED)
            saveState(finished, onEvent)
            onEvent(CampaignEvent.Finished(finished))
            KaiResult.Success(finished)
        } catch (error: Throwable) {
            val failed = state.copy(status = CampaignStatus.FAILED)
            saveState(failed, onEvent)
            onEvent(CampaignEvent.Failed(failed, error.message ?: error::class.simpleName.orEmpty()))
            KaiResult.Failure(error.message ?: error::class.simpleName.orEmpty(), error)
        }
    }

    private fun validate(config: CampaignConfig): KaiResult<Unit> {
        when (val result = registry.validateConfig(config)) {
            ValidationResult.Success -> return KaiResult.Success(Unit)
            is ValidationResult.Failure -> return KaiResult.Failure(result.errors.joinToString("\n"))
        }
    }

    private fun hasBudget(state: CampaignState): Boolean {
        return state.stats.totalGenerated < state.config.budget.maxIterations
    }

    private fun nextTestCase(
        state: CampaignState,
        signals: Map<kai.domain.id.StrategyId, List<kai.plugin.strategy.FitnessSignal>>
    ): TestCase {
        val scheduler = registry.resolveScheduler()
        val strategyId = scheduler.next(state.config.strategyIds, signals, state.stats)
        val strategy = registry.resolveStrategy(strategyId.value)
        val corpus = corpusSample(state.config)
        val context = GenerationContext(
            campaignId = state.id,
            seed = state.stats.totalGenerated.toLong() + 1L,
            iteration = state.stats.totalGenerated,
            compilerProfiles = state.config.compilerProfiles,
            corpusSample = corpus.take(8)
        )
        return strategy.generate(context).first()
    }

    private fun storeTestCase(
        testCase: TestCase,
        onEvent: (CampaignEvent) -> Unit
    ) {
        storage.corpus.saveTestCase(testCase).getOrThrow()
        onEvent(CampaignEvent.TestCaseGenerated(testCase))
    }

    private fun execute(
        state: CampaignState,
        testCase: TestCase
    ): Observations {
        val executor = registry.resolveExecutor(state.config.executorId.value)
        val results = executor.execute(testCase, state.config.executionConfig)
        return Observations.create(testCase.id, results)
    }

    private fun evaluate(
        config: CampaignConfig,
        observations: Observations,
        testCase: TestCase,
        reduction: ConfiguredReductionService,
        onEvent: (CampaignEvent) -> Unit
    ): List<Finding> {
        val findings = mutableListOf<Finding>()
        config.oracleIds.forEach { oracleId ->
            val oracle = registry.resolveOracle(oracleId.value)
            val verdict = oracle.check(observations, testCase)
            if (verdict is OracleVerdict.Interesting) {
                val finding = reduction.reduce(verdict)
                if (isNewFinding(finding)) {
                    storage.findings.saveFinding(finding).getOrThrow()
                    onEvent(CampaignEvent.FindingStored(finding))
                    findings += finding
                }
            }
        }
        return findings
    }

    private fun isNewFinding(finding: Finding): Boolean {
        return storage.findings.loadFinding(finding.id).getOrThrow() == null
    }

    private fun corpusSample(config: CampaignConfig): List<TestCase> {
        val seeded = config.seedCorpusIds.mapNotNull { id ->
            storage.corpus.loadTestCase(kai.domain.id.TestCaseId(id)).getOrThrow()
        }
        val corpus = if (seeded.isEmpty()) storage.corpus.listTestCases().getOrThrow() else seeded
        return corpus.take(8)
    }

    private fun updateSignals(
        observations: Observations,
        testCase: TestCase,
        signals: MutableMap<kai.domain.id.StrategyId, MutableList<kai.plugin.strategy.FitnessSignal>>
    ) {
        val strategy = registry.resolveStrategy(testCase.provenance.strategyId.value)
        val feedback = strategy.feedback(observations, testCase)
        val bucket = signals.getOrPut(strategy.id) { mutableListOf() }
        bucket += feedback
    }

    private fun advance(
        state: CampaignState,
        newFindings: Int
    ): CampaignState {
        val current = state.stats
        val nextStats = CampaignStats(
            totalGenerated = current.totalGenerated + 1,
            totalExecuted = current.totalExecuted + 1,
            totalInteresting = current.totalInteresting + newFindings
        )
        return state.copy(status = CampaignStatus.RUNNING, stats = nextStats)
    }

    private fun saveState(
        state: CampaignState,
        onEvent: (CampaignEvent) -> Unit
    ) {
        storage.campaigns.saveCampaign(state).getOrThrow()
        onEvent(CampaignEvent.StateSaved(state))
    }
}
