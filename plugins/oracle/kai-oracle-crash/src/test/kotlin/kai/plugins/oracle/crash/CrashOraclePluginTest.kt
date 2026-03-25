package kai.plugins.oracle.crash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
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

class CrashOraclePluginTest {
    private val plugin = CrashOraclePlugin()

    @Test
    fun `crash oracle creates stable signature for repeated ICE`() {
        val testCase = sampleTestCase()
        val stderr = """
            internal error
            org.jetbrains.kotlin.codegen.CompilationException: boom
            at org.jetbrains.kotlin.backend.Main.lower
            at org.jetbrains.kotlin.backend.Main.generate
        """.trimIndent()
        val observations = Observations.create(
            testCase.id,
            listOf(
                ExecutionResult.create(
                    profileName = "default",
                    command = listOf("kotlinc", "Main.kt"),
                    exitCode = 1,
                    stdout = "",
                    stderr = stderr,
                    durationMs = 10
                )
            )
        )

        val first = plugin.check(observations, testCase)
        val second = plugin.check(observations, testCase)

        val firstFinding = assertIs<OracleVerdict.Interesting>(first).finding
        val secondFinding = assertIs<OracleVerdict.Interesting>(second).finding
        assertEquals(firstFinding.signature.hash, secondFinding.signature.hash)
        assertEquals(listOf("at org.jetbrains.kotlin.backend.Main.lower", "at org.jetbrains.kotlin.backend.Main.generate"), firstFinding.signature.normalizedFrames)
        assertTrue(firstFinding.evidence.stackTrace!!.contains("CompilationException"))
    }

    private fun sampleTestCase(): TestCase {
        return TestCase.create(
            sources = listOf(SourceFile.create("Main.kt", "fun main() = println(1)")),
            buildConfig = BuildConfig.create(
                compilerProfiles = listOf(CompilerProfile.create("default"))
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
