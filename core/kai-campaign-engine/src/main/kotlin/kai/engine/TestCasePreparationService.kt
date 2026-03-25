package kai.engine

import kai.domain.result.KaiResult
import kai.domain.result.kaiResult
import kai.domain.testcase.SourceFile
import kai.domain.testcase.TestCase
import kai.plugin.executor.ExecutorPlugin

class TestCasePreparationService {
    fun prepare(
        testCase: TestCase,
        executor: ExecutorPlugin
    ): KaiResult<TestCase> = kaiResult {
        val normalized = normalize(testCase)
        validate(normalized, executor)
        normalized
    }

    private fun normalize(testCase: TestCase): TestCase {
        val normalizedSources = testCase.sources.map { source ->
            SourceFile.create(
                relativePath = normalizePath(source.relativePath),
                content = normalizeContent(source.content)
            )
        }
        require(normalizedSources.map { it.relativePath }.distinct().size == normalizedSources.size) {
            "test case contains duplicate source paths"
        }
        return TestCase.create(
            sources = normalizedSources,
            buildConfig = testCase.buildConfig,
            provenance = testCase.provenance
        )
    }

    private fun validate(
        testCase: TestCase,
        executor: ExecutorPlugin
    ) {
        require(executor.capabilities.supports(testCase.buildConfig.target)) {
            "Executor ${executor.id.value} does not support target ${testCase.buildConfig.target.name}"
        }
    }

    private fun normalizePath(path: String): String {
        return path.replace('\\', '/').trim()
    }

    private fun normalizeContent(content: String): String {
        val normalized = content
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .map(String::trimEnd)
            .joinToString("\n")
            .trimEnd()
        require(normalized.isNotBlank()) { "source content must not be blank" }
        return normalized
    }
}
