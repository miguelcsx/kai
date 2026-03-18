package kai.domain.testcase

import java.security.MessageDigest
import kai.domain.id.CampaignId
import kai.domain.id.StrategyId
import kai.domain.id.TestCaseId

data class SourceFile(
    val relativePath: String,
    val content: String
) {
    companion object {
        fun create(relativePath: String, content: String): SourceFile {
            require(relativePath.isNotBlank()) { "relativePath must not be blank" }
            return SourceFile(relativePath, content)
        }
    }
}

data class CompilerProfile(
    val name: String,
    val binary: String,
    val flags: List<String>
) {
    companion object {
        fun create(
            name: String,
            binary: String = "kotlinc",
            flags: List<String> = emptyList()
        ): CompilerProfile {
            require(name.isNotBlank()) { "name must not be blank" }
            require(binary.isNotBlank()) { "binary must not be blank" }
            return CompilerProfile(name, binary, flags.toList())
        }
    }
}

data class BuildConfig(
    val compilerProfiles: List<CompilerProfile>,
    val target: String,
    val languageVersion: String,
    val apiVersion: String
) {
    companion object {
        fun create(
            compilerProfiles: List<CompilerProfile>,
            target: String = "JVM",
            languageVersion: String = "default",
            apiVersion: String = "default"
        ): BuildConfig {
            require(compilerProfiles.isNotEmpty()) { "compilerProfiles must not be empty" }
            return BuildConfig(
                compilerProfiles = compilerProfiles.toList(),
                target = target,
                languageVersion = languageVersion,
                apiVersion = apiVersion
            )
        }
    }
}

data class Provenance(
    val campaignId: CampaignId,
    val strategyId: StrategyId,
    val parentId: TestCaseId?,
    val generationSeed: Long,
    val mutationDepth: Int
)

data class TestCase(
    val id: TestCaseId,
    val sources: List<SourceFile>,
    val buildConfig: BuildConfig,
    val provenance: Provenance
) {
    val charCount: Int
        get() = sources.sumBy { it.content.length }

    companion object {
        fun create(
            sources: List<SourceFile>,
            buildConfig: BuildConfig,
            provenance: Provenance
        ): TestCase {
            require(sources.isNotEmpty()) { "sources must not be empty" }
            return TestCase(
                id = TestCaseId(hash(sources, buildConfig)),
                sources = sources.toList(),
                buildConfig = buildConfig,
                provenance = provenance
            )
        }

        private fun hash(
            sources: List<SourceFile>,
            buildConfig: BuildConfig
        ): String {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(renderSources(sources).toByteArray())
            digest.update(renderProfiles(buildConfig.compilerProfiles).toByteArray())
            return digest.digest()
                .joinToString("") { "%02x".format(it) }
                .take(16)
        }

        private fun renderSources(sources: List<SourceFile>): String {
            return sources.sortedBy { it.relativePath }
                .joinToString("\n") { it.relativePath + "\n" + it.content }
        }

        private fun renderProfiles(profiles: List<CompilerProfile>): String {
            return profiles.joinToString("\n") {
                listOf(it.name, it.binary, it.flags.joinToString(",")).joinToString("|")
            }
        }
    }
}
