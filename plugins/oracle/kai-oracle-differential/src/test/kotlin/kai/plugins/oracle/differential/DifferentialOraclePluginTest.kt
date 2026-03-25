package kai.plugins.oracle.differential

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kai.domain.finding.OracleVerdict
import kai.domain.id.CampaignId
import kai.domain.id.StrategyId
import kai.domain.observations.ExecutionResult
import kai.domain.observations.Observations
import kai.domain.testcase.BuildConfig
import kai.domain.testcase.CompilerProfile
import kai.domain.testcase.Provenance
import kai.domain.testcase.SourceFile
import kai.domain.testcase.TestCase

class DifferentialOraclePluginTest {
    private val plugin = DifferentialOraclePlugin()

    @Test
    fun `differential oracle reports stable mismatch signature`() {
        val testCase = sampleTestCase()
        val observations = Observations.create(
            testCase.id,
            listOf(
                ExecutionResult.create(
                    profileName = "default",
                    command = listOf("kotlinc", "Main.kt"),
                    exitCode = 0,
                    stdout = "",
                    stderr = "",
                    durationMs = 10
                ),
                ExecutionResult.create(
                    profileName = "strict",
                    command = listOf("kotlinc", "-Werror", "Main.kt"),
                    exitCode = 1,
                    stdout = "",
                    stderr = "Main.kt:1: warning: unstable behavior",
                    durationMs = 12
                )
            )
        )

        val first = assertIs<OracleVerdict.Interesting>(plugin.check(observations, testCase)).finding
        val second = assertIs<OracleVerdict.Interesting>(plugin.check(observations, testCase)).finding

        assertEquals(first.signature.hash, second.signature.hash)
        assertEquals("default:strict", first.signature.profileName)
        assertEquals(first.evidence.divergence, second.evidence.divergence)
    }

    private fun sampleTestCase(): TestCase {
        return TestCase.create(
            sources = listOf(SourceFile.create("Main.kt", "fun main() = println(1)")),
            buildConfig = BuildConfig.create(
                compilerProfiles = listOf(
                    CompilerProfile.create("default"),
                    CompilerProfile.create("strict", flags = listOf("-Werror"))
                )
            ),
            provenance = Provenance(
                campaignId = CampaignId("campaign"),
                strategyId = StrategyId("random-generator"),
                parentId = null,
                generationSeed = 1L,
                mutationDepth = 0
            )
        )
    }
}
