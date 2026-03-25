package kai.engine

import kai.domain.campaign.CampaignConfig
import kai.domain.campaign.CampaignState
import kai.domain.campaign.CampaignStats
import kai.domain.campaign.CampaignStatus
import kai.domain.execution.ExecutionConfig
import kai.domain.id.StrategyId
import kai.domain.finding.Finding
import kai.domain.finding.OracleVerdict
import kai.domain.observations.Observations
import kai.domain.result.KaiResult
import kai.domain.result.getOrThrow
import kai.domain.testcase.TestCase
import kai.engine.event.CampaignEvent
import kai.plugin.executor.ExecutorPlugin
import kai.plugin.oracle.OraclePlugin
import kai.plugin.registry.PluginRegistry
import kai.plugin.registry.ValidationResult
import kai.plugin.scheduler.SchedulerPlugin
import kai.plugin.strategy.FitnessSignal
import kai.plugin.strategy.GenerationContext
import kai.plugin.strategy.StrategyPlugin
import kai.storage.api.StoragePorts

class CampaignEngine(
    private val registry: PluginRegistry,
    private val storage: StoragePorts,
    private val verifier: FindingVerifier = OracleFindingVerifier(registry),
    private val reductionService: ReductionService = ReductionService(registry, verifier),
    private val preparationService: TestCasePreparationService = TestCasePreparationService()
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
            val runtime = runtime(config)
            saveState(state, onEvent)
            onEvent(CampaignEvent.Started(state))
            val signals = linkedMapOf<StrategyId, MutableList<FitnessSignal>>()
            while (hasBudget(state)) {
                state = state.copy(status = CampaignStatus.RUNNING)
                val testCase = prepareTestCase(nextTestCase(state, runtime, signals), runtime.executor).getOrThrow()
                storeTestCase(testCase, onEvent)
                val observations = execute(testCase, runtime.executor, state.config.executionConfig)
                val findings = evaluate(observations, testCase, runtime, onEvent)
                updateSignals(observations, testCase, runtime, signals)
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

    private fun runtime(config: CampaignConfig): CampaignRuntime {
        return CampaignRuntime(
            config = config,
            scheduler = registry.resolveScheduler(),
            strategies = config.strategyIds.associateWith { strategyId -> registry.resolveStrategy(strategyId.value) },
            executor = registry.resolveExecutor(config.executorId.value),
            oracles = config.oracleIds.map { oracleId -> registry.resolveOracle(oracleId.value) },
            reduction = reductionService.forConfig(config)
        )
    }

    private fun nextTestCase(
        state: CampaignState,
        runtime: CampaignRuntime,
        signals: Map<StrategyId, List<FitnessSignal>>
    ): TestCase {
        val strategyId = runtime.scheduler.next(state.config.strategyIds, signals, state.stats)
        val strategy = runtime.strategies.getValue(strategyId)
        val corpus = corpusSample(runtime.config)
        val context = GenerationContext(
            campaignId = state.id,
            seed = state.stats.totalGenerated.toLong() + 1L,
            iteration = state.stats.totalGenerated,
            buildConfig = runtime.config.buildConfig,
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

    private fun prepareTestCase(
        testCase: TestCase,
        executor: ExecutorPlugin
    ): KaiResult<TestCase> {
        return preparationService.prepare(testCase, executor)
    }

    private fun execute(testCase: TestCase, executor: ExecutorPlugin, config: ExecutionConfig): Observations {
        val results = executor.execute(testCase, config)
        return Observations.create(testCase.id, results)
    }

    private fun evaluate(
        observations: Observations,
        testCase: TestCase,
        runtime: CampaignRuntime,
        onEvent: (CampaignEvent) -> Unit
    ): List<Finding> {
        val findings = mutableListOf<Finding>()
        runtime.oracles.forEach { oracle ->
            val verdict = oracle.check(observations, testCase)
            if (verdict is OracleVerdict.Interesting) {
                val finding = runtime.reduction.reduce(verdict)
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
            storage.corpus.loadTestCase(id).getOrThrow()
        }
        val corpus = if (seeded.isEmpty()) storage.corpus.listTestCases().getOrThrow() else seeded
        return corpus.take(8)
    }

    private fun updateSignals(
        observations: Observations,
        testCase: TestCase,
        runtime: CampaignRuntime,
        signals: MutableMap<StrategyId, MutableList<FitnessSignal>>
    ) {
        val strategy = runtime.strategies.getValue(testCase.provenance.strategyId)
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

private data class CampaignRuntime(
    val config: CampaignConfig,
    val scheduler: SchedulerPlugin,
    val strategies: Map<StrategyId, StrategyPlugin>,
    val executor: ExecutorPlugin,
    val oracles: List<OraclePlugin>,
    val reduction: ConfiguredReductionService
)
