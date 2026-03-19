package kai.plugin.strategy

import kai.domain.id.CampaignId
import kai.domain.id.InterfaceVersion
import kai.domain.id.StrategyId
import kai.domain.observations.Observations
import kai.domain.testcase.CompilerProfile
import kai.domain.testcase.TestCase

data class GenerationContext(
    val campaignId: CampaignId,
    val seed: Long,
    val iteration: Int,
    val compilerProfiles: List<CompilerProfile>,
    val corpusSample: List<TestCase>
)

data class MutationContext(
    val campaignId: CampaignId,
    val seed: Long,
    val compilerProfiles: List<CompilerProfile>,
    val iteration: Int
)

data class FitnessSignal(
    val score: Double,
    val label: String?
) {
    companion object {
        fun neutral(): FitnessSignal {
            return FitnessSignal(0.0, null)
        }
    }
}

interface StrategyPlugin {
    val id: StrategyId
    val version: InterfaceVersion

    fun generate(context: GenerationContext): Sequence<TestCase>

    fun mutate(seed: TestCase, context: MutationContext): TestCase

    fun feedback(observations: Observations, testCase: TestCase): FitnessSignal
}
