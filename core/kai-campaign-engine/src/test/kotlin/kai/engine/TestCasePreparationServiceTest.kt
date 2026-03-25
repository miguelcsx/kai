package kai.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kai.domain.execution.ExecutionConfig
import kai.domain.execution.ExecutorCapabilities
import kai.domain.execution.ExecutorProbeResult
import kai.domain.id.CampaignId
import kai.domain.id.ExecutorId
import kai.domain.id.InterfaceVersion
import kai.domain.id.StrategyId
import kai.domain.result.KaiResult
import kai.domain.testcase.BuildConfig
import kai.domain.testcase.CompilationTarget
import kai.domain.testcase.CompilerProfile
import kai.domain.testcase.Provenance
import kai.domain.testcase.SourceFile
import kai.domain.testcase.TestCase
import kai.plugin.executor.ExecutorPlugin

class TestCasePreparationServiceTest {
    private val service = TestCasePreparationService()

    @Test
    fun `preparation normalizes source paths and content`() {
        val testCase = TestCase.create(
            sources = listOf(SourceFile.create(" src\\Main.kt ", "fun main() {\r\n    println(1)    \r\n}\r\n")),
            buildConfig = BuildConfig.create(
                compilerProfiles = listOf(CompilerProfile.create("default"))
            ),
            provenance = provenance()
        )

        val prepared = assertIs<KaiResult.Success<TestCase>>(service.prepare(testCase, FakeExecutor())).value

        assertEquals("src/Main.kt", prepared.sources.single().relativePath)
        assertEquals("fun main() {\n    println(1)\n}", prepared.sources.single().content)
    }

    @Test
    fun `preparation rejects unsupported compilation target`() {
        val testCase = TestCase.create(
            sources = listOf(SourceFile.create("Main.kt", "fun main() = println(1)")),
            buildConfig = BuildConfig.create(
                compilerProfiles = listOf(CompilerProfile.create("default")),
                target = CompilationTarget.JS
            ),
            provenance = provenance()
        )

        val result = service.prepare(testCase, FakeExecutor())

        val failure = assertIs<KaiResult.Failure>(result)
        assertTrue(failure.message.contains("does not support target JS"))
    }

    private fun provenance(): Provenance {
        return Provenance(
            campaignId = CampaignId("campaign"),
            strategyId = StrategyId("random-generator"),
            parentId = null,
            generationSeed = 1L,
            mutationDepth = 0
        )
    }
}

private class FakeExecutor : ExecutorPlugin {
    override val id = ExecutorId("fake-executor")
    override val version = InterfaceVersion.V1
    override val capabilities = ExecutorCapabilities.jvmOnly()

    override fun execute(testCase: TestCase, config: ExecutionConfig) = emptyList<kai.domain.observations.ExecutionResult>()

    override fun probe() = ExecutorProbeResult.Available("ready")
}
