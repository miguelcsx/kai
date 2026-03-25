package kai.plugins.executor.cli

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kai.domain.execution.ExecutionConfig
import kai.domain.execution.ExecutorCapabilities
import kai.domain.execution.ExecutorProbeResult
import kai.domain.id.ExecutorId
import kai.domain.id.InterfaceVersion
import kai.domain.observations.ArtifactKind
import kai.domain.observations.CompilationArtifact
import kai.domain.observations.CompilerDiagnostic
import kai.domain.observations.DiagnosticSeverity
import kai.domain.observations.ExecutionResult
import kai.domain.testcase.TestCase
import kai.plugin.executor.ExecutorPlugin

class CliExecutorPlugin : ExecutorPlugin {
    override val id = ExecutorId("cli-executor")
    override val version = InterfaceVersion.V1
    override val capabilities = ExecutorCapabilities.jvmOnly()

    override fun execute(testCase: TestCase, config: ExecutionConfig): List<ExecutionResult> {
        return testCase.buildConfig.compilerProfiles.map { profile ->
            executeProfile(testCase, profile.binary, profile.name, profile.flags, config)
        }
    }

    override fun probe(): ExecutorProbeResult {
        return try {
            val process = ProcessBuilder("kotlinc", "-version")
                .redirectErrorStream(true)
                .start()
            process.waitFor(5, TimeUnit.SECONDS)
            ExecutorProbeResult.Available("kotlinc is available")
        } catch (error: Exception) {
            ExecutorProbeResult.Unavailable(error.message ?: "kotlinc probe failed")
        }
    }

    private fun executeProfile(
        testCase: TestCase,
        binary: String,
        profileName: String,
        flags: List<String>,
        config: ExecutionConfig
    ): ExecutionResult {
        val workDir = Files.createTempDirectory("kai-exec-")
        return try {
            val sources = writeSources(workDir, testCase)
            val outputDir = Files.createDirectory(workDir.resolve("out"))
            val command = buildList {
                add(binary)
                addAll(flags)
                addAll(sources.map(Path::toString))
                add("-d")
                add(outputDir.toString())
            }
            val process = ProcessBuilder(command)
                .directory(workDir.toFile())
                .apply { environment().putAll(config.environment) }
                .start()
            val stdout = readAsync(process.inputStream)
            val stderr = readAsync(process.errorStream)
            val startedAt = System.nanoTime()
            val completed = process.waitFor(config.timeoutMillis, TimeUnit.MILLISECONDS)
            val durationMs = (System.nanoTime() - startedAt) / 1_000_000L
            if (!completed) {
                process.destroyForcibly()
                stdout.cancel(true)
                stderr.cancel(true)
                return timeoutResult(profileName, command, durationMs)
            }
            val stdoutText = stdout.join()
            val stderrText = stderr.join()
            ExecutionResult.create(
                profileName = profileName,
                command = command,
                exitCode = process.exitValue(),
                stdout = stdoutText,
                stderr = stderrText,
                durationMs = durationMs,
                diagnostics = diagnostics(stderrText),
                artifacts = if (config.captureArtifacts) artifacts(outputDir) else emptyList()
            )
        } finally {
            workDir.toFile().deleteRecursively()
        }
    }

    private fun writeSources(workDir: Path, testCase: TestCase): List<Path> {
        return testCase.sources.map { source ->
            workDir.resolve(source.relativePath).also { path ->
                path.parent?.let(Files::createDirectories)
                Files.writeString(path, source.content)
            }
        }
    }

    private fun timeoutResult(
        profileName: String,
        command: List<String>,
        durationMs: Long
    ): ExecutionResult {
        return ExecutionResult.create(
            profileName = profileName,
            command = command,
            exitCode = 124,
            stdout = "",
            stderr = "timeout",
            durationMs = durationMs,
            diagnostics = listOf(CompilerDiagnostic(DiagnosticSeverity.ERROR, "timeout", null))
        )
    }

    private fun diagnostics(stderr: String): List<CompilerDiagnostic> {
        return stderr.lines()
            .filter { it.contains("error", ignoreCase = true) || it.contains("warning", ignoreCase = true) }
            .map { line ->
                val severity = if (line.contains("warning", ignoreCase = true)) "WARNING" else "ERROR"
                CompilerDiagnostic(DiagnosticSeverity.valueOf(severity), line.trim(), null)
            }
    }

    private fun artifacts(outputDir: Path): List<CompilationArtifact> {
        return Files.walk(outputDir).use { paths ->
            paths.filter(Files::isRegularFile)
                .map { path ->
                    CompilationArtifact(
                        kind = artifactKind(path),
                        path = outputDir.relativize(path).toString(),
                        sizeBytes = Files.size(path)
                    )
                }
                .toList()
        }
    }

    private fun artifactKind(path: Path): ArtifactKind {
        return when (path.fileName.toString().substringAfterLast('.', "")) {
            "class" -> ArtifactKind.CLASSFILE
            "js" -> ArtifactKind.JS
            "klib", "so", "dylib", "dll", "exe" -> ArtifactKind.NATIVE_BINARY
            else -> ArtifactKind.IR
        }
    }

    private fun readAsync(stream: InputStream): CompletableFuture<String> {
        return CompletableFuture.supplyAsync {
            stream.bufferedReader().use { it.readText() }
        }
    }
}
