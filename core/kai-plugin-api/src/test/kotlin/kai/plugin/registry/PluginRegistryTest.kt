package kai.plugin.registry

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kai.domain.campaign.CampaignBudget
import kai.domain.campaign.CampaignConfig
import kai.domain.execution.ExecutionConfig
import kai.domain.execution.ExecutorCapabilities
import kai.domain.execution.ExecutorProbeResult
import kai.domain.finding.FindingKind
import kai.domain.id.CampaignId
import kai.domain.id.ExecutorId
import kai.domain.id.InterfaceVersion
import kai.domain.id.OracleId
import kai.domain.id.ReducerId
import kai.domain.id.StrategyId
import kai.domain.testcase.BuildConfig
import kai.domain.observations.Observations
import kai.domain.testcase.TestCase
import kai.plugin.executor.ExecutorPlugin
import kai.plugin.oracle.OraclePlugin
import kai.plugin.reducer.InterestingnessPredicate
import kai.plugin.reducer.ReducedTestCase
import kai.plugin.reducer.ReductionBudget
import kai.plugin.reducer.ReducerPlugin
import kai.plugin.scheduler.SchedulerPlugin
import kai.plugin.strategy.FitnessSignal
import kai.plugin.strategy.GenerationContext
import kai.plugin.strategy.MutationContext
import kai.plugin.strategy.StrategyPlugin

class PluginRegistryTest {
    @Test
    fun `config validation fails when executor probe is unavailable`() {
        val registry = PluginRegistry()
        registry.registerStrategy(FakeStrategy())
        registry.registerExecutor(UnavailableExecutor())
        registry.registerOracle(FakeOracle())
        registry.registerReducer(FakeReducer())
        registry.registerScheduler(FakeScheduler())

        val validation = registry.validateConfig(config())

        val failure = assertIs<ValidationResult.Failure>(validation)
        assertTrue(failure.errors.any { it.contains("Executor plugin unavailable") })
    }

    private fun config(): CampaignConfig {
        return CampaignConfig(
            id = CampaignId("campaign"),
            strategyIds = listOf(StrategyId("fake-strategy")),
            executorId = ExecutorId("fake-executor"),
            oracleIds = listOf(OracleId("fake-oracle")),
            reducerIds = listOf(ReducerId("fake-reducer")),
            buildConfig = BuildConfig.create(
                compilerProfiles = listOf(kai.domain.testcase.CompilerProfile.create("default"))
            ),
            budget = CampaignBudget.create(1, 1, 1),
            seedCorpusIds = emptyList(),
            executionConfig = ExecutionConfig.create(1000)
        )
    }
}

private class FakeStrategy : StrategyPlugin {
    override val id = StrategyId("fake-strategy")
    override val version = InterfaceVersion.V1
    override fun generate(context: GenerationContext): Sequence<TestCase> = emptySequence()
    override fun mutate(seed: TestCase, context: MutationContext): TestCase = seed
    override fun feedback(observations: Observations, testCase: TestCase): FitnessSignal = FitnessSignal.neutral()
}

private class UnavailableExecutor : ExecutorPlugin {
    override val id = ExecutorId("fake-executor")
    override val version = InterfaceVersion.V1
    override val capabilities = ExecutorCapabilities.jvmOnly()
    override fun execute(testCase: TestCase, config: ExecutionConfig) = emptyList<kai.domain.observations.ExecutionResult>()
    override fun probe() = ExecutorProbeResult.Unavailable("missing binary")
}

private class FakeOracle : OraclePlugin {
    override val id = OracleId("fake-oracle")
    override val version = InterfaceVersion.V1
    override val kind = FindingKind.CRASH
    override fun check(observations: Observations, testCase: TestCase) = kai.domain.finding.OracleVerdict.NotInteresting
}

private class FakeReducer : ReducerPlugin {
    override val id = ReducerId("fake-reducer")
    override val version = InterfaceVersion.V1
    override fun reduce(testCase: TestCase, predicate: InterestingnessPredicate, budget: ReductionBudget): ReducedTestCase {
        return ReducedTestCase(testCase, testCase, 0)
    }
}

private class FakeScheduler : SchedulerPlugin {
    override fun next(
        strategies: List<StrategyId>,
        signals: Map<StrategyId, List<FitnessSignal>>,
        stats: kai.domain.campaign.CampaignStats
    ): StrategyId = strategies.first()
}
