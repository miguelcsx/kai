package kai.storage.fs

import kai.domain.id.CampaignId
import kai.domain.id.StrategyId
import kai.domain.id.TestCaseId
import kai.domain.testcase.BuildConfig
import kai.domain.testcase.CompilerProfile
import kai.domain.testcase.Provenance
import kai.domain.testcase.SourceFile
import kai.domain.testcase.TestCase

object TestCaseCodec {
    fun encode(testCase: TestCase): JsonValue {
        return JsonValue.Obj(
            linkedMapOf(
                "id" to JsonValue.Str(testCase.id.value),
                "sources" to encodeSources(testCase),
                "buildConfig" to encodeBuildConfig(testCase),
                "provenance" to encodeProvenance(testCase)
            )
        )
    }

    fun decode(value: JsonValue): TestCase {
        val root = value.asObject()
        val sources = decodeSources(root.values.getValue("sources"))
        val buildConfig = decodeBuildConfig(root.values.getValue("buildConfig"))
        val provenance = decodeProvenance(root.values.getValue("provenance"))
        return TestCase(TestCaseId(value.string("id")), sources, buildConfig, provenance)
    }

    private fun encodeSources(testCase: TestCase): JsonValue {
        return JsonValue.Arr(testCase.sources.map { source ->
            JsonValue.Obj(
                linkedMapOf(
                    "relativePath" to JsonValue.Str(source.relativePath),
                    "content" to JsonValue.Str(source.content)
                )
            )
        })
    }

    private fun decodeSources(value: JsonValue): List<SourceFile> {
        return value.asArray().map { item ->
            val node = item.asObject()
            SourceFile.create(node.values.getValue("relativePath").asString(), node.values.getValue("content").asString())
        }
    }

    private fun encodeBuildConfig(testCase: TestCase): JsonValue {
        val profiles = testCase.buildConfig.compilerProfiles.map { profile ->
            JsonValue.Obj(
                linkedMapOf(
                    "name" to JsonValue.Str(profile.name),
                    "binary" to JsonValue.Str(profile.binary),
                    "flags" to JsonValue.Arr(profile.flags.map { JsonValue.Str(it) })
                )
            )
        }
        return JsonValue.Obj(
            linkedMapOf(
                "profiles" to JsonValue.Arr(profiles),
                "target" to JsonValue.Str(testCase.buildConfig.target),
                "languageVersion" to JsonValue.Str(testCase.buildConfig.languageVersion),
                "apiVersion" to JsonValue.Str(testCase.buildConfig.apiVersion)
            )
        )
    }

    private fun decodeBuildConfig(value: JsonValue): BuildConfig {
        val node = value.asObject()
        val profiles = node.values.getValue("profiles").asArray().map { item ->
            val profile = item.asObject()
            CompilerProfile.create(
                name = profile.values.getValue("name").asString(),
                binary = profile.values.getValue("binary").asString(),
                flags = profile.values.getValue("flags").asArray().map { it.asString() }
            )
        }
        return BuildConfig.create(
            compilerProfiles = profiles,
            target = node.values.getValue("target").asString(),
            languageVersion = node.values.getValue("languageVersion").asString(),
            apiVersion = node.values.getValue("apiVersion").asString()
        )
    }

    private fun encodeProvenance(testCase: TestCase): JsonValue {
        val parent = testCase.provenance.parentId
        return JsonValue.Obj(
            linkedMapOf(
                "campaignId" to JsonValue.Str(testCase.provenance.campaignId.value),
                "strategyId" to JsonValue.Str(testCase.provenance.strategyId.value),
                "parentId" to if (parent == null) JsonValue.Nil else JsonValue.Str(parent.value),
                "generationSeed" to JsonValue.Num(testCase.provenance.generationSeed),
                "mutationDepth" to JsonValue.Num(testCase.provenance.mutationDepth.toLong())
            )
        )
    }

    private fun decodeProvenance(value: JsonValue): Provenance {
        val node = value.asObject()
        val parent = node.values["parentId"]
        return Provenance(
            campaignId = CampaignId(node.values.getValue("campaignId").asString()),
            strategyId = StrategyId(node.values.getValue("strategyId").asString()),
            parentId = if (parent == null || parent is JsonValue.Nil) null else TestCaseId(parent.asString()),
            generationSeed = node.values.getValue("generationSeed").asLong(),
            mutationDepth = node.values.getValue("mutationDepth").asLong().toInt()
        )
    }
}
