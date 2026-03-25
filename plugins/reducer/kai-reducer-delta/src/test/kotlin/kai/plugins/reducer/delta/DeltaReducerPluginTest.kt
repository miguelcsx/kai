package kai.plugins.reducer.delta

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kai.domain.id.CampaignId
import kai.domain.id.StrategyId
import kai.domain.testcase.BuildConfig
import kai.domain.testcase.CompilerProfile
import kai.domain.testcase.Provenance
import kai.domain.testcase.SourceFile
import kai.domain.testcase.TestCase
import kai.plugin.reducer.InterestingnessPredicate
import kai.plugin.reducer.ReductionBudget

class DeltaReducerPluginTest {
    private val plugin = DeltaReducerPlugin()

    @Test
    fun `delta reducer keeps interesting testcase while shrinking content`() {
        val testCase = TestCase.create(
            sources = listOf(
                SourceFile.create(
                    "Main.kt",
                    """
                    fun helper() {
                        println("helper")
                    }

                    fun main() {
                        helper()
                    }

                    val noisy = 1
                    """.trimIndent()
                )
            ),
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

        val reduced = plugin.reduce(
            testCase,
            object : InterestingnessPredicate {
                override fun isSatisfied(candidate: TestCase): Boolean {
                    return candidate.sources.first().content.contains("fun main")
                }
            },
            ReductionBudget(maxIterations = 8)
        )

        assertTrue(reduced.iterations > 0)
        assertTrue(reduced.reduced.charCount < reduced.original.charCount)
        assertTrue(reduced.reduced.sources.first().content.contains("fun main"))
        assertEquals("Main.kt", reduced.reduced.sources.first().relativePath)
    }
}
