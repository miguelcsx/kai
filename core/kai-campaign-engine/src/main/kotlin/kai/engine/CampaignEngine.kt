package kai.engine

import kai.domain.campaign.CampaignConfig
import kai.domain.campaign.CampaignState
import kai.domain.campaign.CampaignStats
import kai.domain.campaign.CampaignStatus
import kai.domain.finding.Finding
import kai.domain.finding.PendingFinding
import kai.domain.finding.OracleVerdict
import kai.domain.observations.Observations
import kai.domain.testcase.TestCase
import kai.engine.event.CampaignEvent
import kai.plugin.reducer.InterestingnessPredicate
import kai.plugin.reducer.ReductionBudget
import kai.plugin.registry.PluginRegistry
import kai.plugin.registry.ValidationResult
import kai.plugin.strategy.GenerationContext
import kai.storage.api.StoragePorts

class CampaignEngine(
    private val registry: PluginRegistry,
    private val storage: StoragePorts
) {
    fun run(
        config: CampaignConfig,
        onEvent: (CampaignEvent) -> Unit
    ): Result<CampaignState> {
        return runCatching {
            validate(config)
            var state = CampaignState.initial(config)
            saveState(state, onEvent)
            onEvent(CampaignEvent.Started(state))
            val signals = linkedMapOf<kai.domain.id.StrategyId, MutableList<kai.plugin.strategy.FitnessSignal>>()
            while (hasBudget(state)) {
                state = state.copy(status = CampaignStatus.RUNNING)
                val testCase = nextTestCase(state, signals)
                storeTestCase(testCase, onEvent)
                val observations = execute(state, testCase)
                val findings = evaluate(state.config, observations, testCase, onEvent)
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
            finished
        }
    }

    private fun validate(config: CampaignConfig) {
        when (val result = registry.validateConfig(config)) {
            ValidationResult.Success -> Unit
            is ValidationResult.Failure -> error(result.errors.joinToString("\n"))
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
        val corpus = storage.corpus.listTestCases().getOrThrow()
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
        onEvent: (CampaignEvent) -> Unit
    ): List<Finding> {
        val findings = mutableListOf<Finding>()
        config.oracleIds.forEach { oracleId ->
            val oracle = registry.resolveOracle(oracleId.value)
            val verdict = oracle.check(observations, testCase)
            if (verdict is OracleVerdict.Interesting) {
                val finding = reduceIfPossible(config, verdict.finding.testCase, verdict)
                if (isNewFinding(finding)) {
                    storage.findings.saveFinding(finding).getOrThrow()
                    onEvent(CampaignEvent.FindingStored(finding))
                    findings += finding
                }
            }
        }
        return findings
    }

    private fun reduceIfPossible(
        config: CampaignConfig,
        testCase: TestCase,
        verdict: OracleVerdict.Interesting
    ): Finding {
        val reducerId = config.reducerIds.firstOrNull() ?: return Finding.create(verdict.finding)
        val reducer = registry.resolveReducer(reducerId.value)
        val predicate = reductionPredicate(config, verdict.finding)
        val reduced = reducer.reduce(testCase, predicate, ReductionBudget(16)).reduced
        val accepted = if (predicate.isSatisfied(reduced)) reduced else null
        return Finding.create(verdict.finding, accepted)
    }

    private fun reductionPredicate(
        config: CampaignConfig,
        original: PendingFinding
    ): InterestingnessPredicate {
        val executor = registry.resolveExecutor(config.executorId.value)
        val oracle = registry.resolveOracle(original.oracleId.value)
        return object : InterestingnessPredicate {
            override fun isSatisfied(candidate: TestCase): Boolean {
                return runCatching {
                    if (candidate.charCount >= original.testCase.charCount) {
                        return@runCatching false
                    }
                    val observations = Observations.create(
                        candidate.id,
                        executor.execute(candidate, config.executionConfig)
                    )
                    val verdict = oracle.check(observations, candidate)
                    sameFinding(original, verdict)
                }.getOrDefault(false)
            }
        }
    }

    private fun sameFinding(
        original: PendingFinding,
        verdict: OracleVerdict
    ): Boolean {
        if (verdict !is OracleVerdict.Interesting) {
            return false
        }
        return verdict.finding.kind == original.kind &&
            verdict.finding.signature.hash == original.signature.hash
    }

    private fun isNewFinding(finding: Finding): Boolean {
        return storage.findings.loadFinding(finding.id).getOrThrow() == null
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
