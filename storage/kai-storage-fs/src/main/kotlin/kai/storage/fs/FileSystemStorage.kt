package kai.storage.fs

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.charset.StandardCharsets
import kai.domain.campaign.CampaignState
import kai.domain.finding.Finding
import kai.domain.id.CampaignId
import kai.domain.id.FindingId
import kai.domain.id.TestCaseId
import kai.domain.testcase.TestCase
import kai.storage.api.CampaignStore
import kai.storage.api.CorpusQuery
import kai.storage.api.CorpusStore
import kai.storage.api.FindingQuery
import kai.storage.api.FindingsStore
import kai.storage.api.StoragePorts

class FileSystemStorage(root: Path) : CorpusStore, FindingsStore, CampaignStore {
    private val baseDir = ensureDirectory(root)
    private val corpusDir = ensureDirectory(baseDir.resolve("corpus"))
    private val findingsDir = ensureDirectory(baseDir.resolve("findings"))
    private val campaignsDir = ensureDirectory(baseDir.resolve("campaigns"))

    val ports = StoragePorts(this, this, this)

    override fun saveTestCase(testCase: TestCase): Result<TestCaseId> = runCatching {
        val file = ensureDirectory(corpusDir.resolve(testCase.id.value)).resolve("testcase.json")
        writeJson(file, TestCaseCodec.encode(testCase))
        testCase.id
    }

    override fun loadTestCase(id: TestCaseId): Result<TestCase?> = runCatching {
        readIfExists(corpusDir.resolve(id.value).resolve("testcase.json")) { TestCaseCodec.decode(it) }
    }

    override fun listTestCases(): Result<List<TestCase>> = runCatching {
        listEntries(corpusDir, "testcase.json").map { readJson(it, TestCaseCodec::decode) }
    }

    override fun findTestCases(query: CorpusQuery): Result<List<TestCase>> = runCatching {
        listTestCases().getOrThrow().filter { testCase ->
            (query.strategyId == null || testCase.provenance.strategyId == query.strategyId) &&
                (query.parentId == null || testCase.provenance.parentId == query.parentId)
        }
    }

    override fun saveFinding(finding: Finding): Result<FindingId> = runCatching {
        val findingDir = ensureDirectory(findingsDir.resolve(finding.id.value))
        writeJson(findingDir.resolve("meta.json"), FindingCodec.encode(finding))
        writeTestCaseTree(findingDir.resolve("testcase"), finding.pending.testCase)
        val reduced = finding.reduced
        if (reduced != null) {
            writeTestCaseTree(findingDir.resolve("reduced"), reduced)
        }
        writeReplayScript(findingDir.resolve("replay.sh"), finding)
        finding.id
    }

    override fun loadFinding(id: FindingId): Result<Finding?> = runCatching {
        readIfExists(findingsDir.resolve(id.value).resolve("meta.json")) { FindingCodec.decode(it) }
    }

    override fun listFindings(): Result<List<Finding>> = runCatching {
        listEntries(findingsDir, "meta.json").map { readJson(it, FindingCodec::decode) }
    }

    override fun findFindings(query: FindingQuery): Result<List<Finding>> = runCatching {
        listFindings().getOrThrow().filter { finding ->
            (query.status == null || finding.status == query.status) &&
                (query.oracleId == null || finding.pending.oracleId == query.oracleId) &&
                (query.kind == null || finding.pending.kind == query.kind)
        }
    }

    override fun saveCampaign(state: CampaignState): Result<CampaignId> = runCatching {
        val file = ensureDirectory(campaignsDir.resolve(state.id.value)).resolve("state.json")
        writeJson(file, CampaignCodec.encode(state))
        state.id
    }

    override fun loadCampaign(id: CampaignId): Result<CampaignState?> = runCatching {
        readIfExists(campaignsDir.resolve(id.value).resolve("state.json")) { CampaignCodec.decode(it) }
    }

    private fun writeTestCaseTree(dir: Path, testCase: TestCase) {
        val target = ensureDirectory(dir)
        writeJson(target.resolve("testcase.json"), TestCaseCodec.encode(testCase))
        val sourceDir = ensureDirectory(target.resolve("src"))
        testCase.sources.forEach { source ->
            writeText(sourceDir.resolve(source.relativePath), source.content)
        }
    }

    private fun writeReplayScript(path: Path, finding: Finding) {
        val testCase = finding.reduced ?: finding.pending.testCase
        val sourceRoot = if (finding.reduced == null) "testcase" else "reduced"
        val profile = testCase.buildConfig.compilerProfiles.first()
        val command = buildList {
            add(profile.binary)
            addAll(profile.flags)
            addAll(testCase.sources.map { "\$ROOT/$sourceRoot/src/${it.relativePath}" })
            add("-d")
            add("\$ROOT/out")
        }
        val content = buildString {
            append("#!/usr/bin/env bash\n")
            append("set -euo pipefail\n")
            append("ROOT=\"$(cd \"$(dirname \"\${BASH_SOURCE[0]}\")\" && pwd)\"\n")
            append("mkdir -p \"\$ROOT/out\"\n")
            append(command.joinToString(" ") { shell(it) })
            append('\n')
        }
        writeText(path, content)
        path.toFile().setExecutable(true)
    }

    private fun writeJson(path: Path, value: JsonValue) {
        writeText(path, JsonWriter.write(value))
    }

    private fun writeText(path: Path, content: String) {
        ensureDirectory(path.parent)
        val temp = path.resolveSibling(path.fileName.toString() + ".tmp")
        Files.writeString(temp, content, StandardCharsets.UTF_8)
        Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun <T> readIfExists(path: Path, decode: (JsonValue) -> T): T? {
        if (!Files.exists(path)) {
            return null
        }
        return readJson(path, decode)
    }

    private fun <T> readJson(path: Path, decode: (JsonValue) -> T): T {
        val content = Files.readString(path, StandardCharsets.UTF_8)
        return decode(JsonParser(content).parse())
    }

    private fun listEntries(base: Path, childName: String): List<Path> {
        if (!Files.exists(base)) {
            return emptyList()
        }
        val stream = Files.list(base)
        try {
            return stream
                .filter { Files.isDirectory(it) }
                .map { it.resolve(childName) }
                .filter { Files.exists(it) }
                .toArray()
                .map { it as Path }
                .sortedBy { it.toString() }
        } finally {
            stream.close()
        }
    }

    private fun ensureDirectory(path: Path): Path {
        return Files.createDirectories(path)
    }

    private fun shell(value: String): String {
        return "'${value.replace("'", "'\"'\"'")}'"
    }
}
