package kai.config

import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kai.domain.result.KaiResult

class TomlCampaignConfigParserTest {
    private val parser = TomlCampaignConfigParser()

    @Test
    fun `demo configuration parses successfully`() {
        val path = locateRepoFile("examples/demo.toml")

        val result = parser.parse(path)

        when (result) {
            is KaiResult.Success -> {
                assertEquals("demo-campaign", result.value.id.value)
                assertEquals(4, result.value.budget.maxIterations)
                assertEquals(4, result.value.budget.maxFindings)
                assertEquals(8, result.value.budget.maxReductionIterations)
                assertEquals(listOf("random-generator", "ast-mutator"), result.value.strategyIds.map { it.value })
                assertEquals(listOf("default", "strict"), result.value.compilerProfiles.map { it.name })
            }
            is KaiResult.Failure -> error("Expected demo config to parse, but failed: ${result.message}")
        }
    }

    @Test
    fun `parser reports missing reduction budget`() {
        val tempFile = kotlin.io.path.createTempFile(suffix = ".toml")
        tempFile.toFile().writeText(
            """
            [campaign]
            id = "broken"

            [campaign.budget]
            max_iterations = 1
            max_findings = 1

            [campaign.strategy]
            ids = ["random-generator"]

            [campaign.executor]
            id = "cli-executor"

            [campaign.oracles]
            ids = ["crash-ice"]

            [campaign.reducers]
            ids = ["delta-debugger"]

            [campaign.profiles]
            ids = ["default"]

            [campaign.profile.default]
            binary = "kotlinc"
            flags = []

            [campaign.seeds]
            corpus_ids = []

            [campaign.execution]
            timeout_ms = 1000
            """.trimIndent()
        )

        val result = parser.parse(tempFile)

        val failure = result as? KaiResult.Failure ?: error("Expected parser failure")
        assertTrue(failure.message.contains("max_reduction_iterations"))
    }

    private fun locateRepoFile(path: String): java.nio.file.Path {
        var current = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        while (current.parent != null) {
            val candidate = current.resolve(path)
            if (candidate.toFile().exists()) {
                return candidate
            }
            current = current.parent
        }
        error("Unable to locate $path from ${System.getProperty("user.dir")}")
    }
}
