package kai.plugins.executor.cli

import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kai.domain.execution.ExecutionConfig
import kai.domain.execution.ExecutorCapabilities
import kai.domain.execution.ExecutorProbeResult
import kai.domain.id.ExecutorId
import kai.domain.id.InterfaceVersion
import kai.domain.observations.CompilerDiagnostic
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
            val source = writeSources(workDir, testCase)
            val outputDir = Files.createDirectory(workDir.resolve("out"))
            val command = mutableListOf(binary)
            command += flags
            command += listOf(source.toString(), "-d", outputDir.toString())
            val startedAt = System.nanoTime()
            val process = ProcessBuilder(command)
                .directory(workDir.toFile())
                .start()
            val completed = process.waitFor(config.timeoutMillis, TimeUnit.MILLISECONDS)
            val durationMs = (System.nanoTime() - startedAt) / 1_000_000L
            if (!completed) {
                process.destroyForcibly()
                return timeoutResult(profileName, command, durationMs)
            }
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            ExecutionResult.create(
                profileName = profileName,
                command = command,
                exitCode = process.exitValue(),
                stdout = stdout,
                stderr = stderr,
                durationMs = durationMs,
                diagnostics = diagnostics(stderr)
            )
        } finally {
            workDir.toFile().deleteRecursively()
        }
    }

    private fun writeSources(workDir: java.nio.file.Path, testCase: TestCase): java.nio.file.Path {
        val source = workDir.resolve(testCase.sources.first().relativePath)
        Files.write(source, testCase.sources.first().content.toByteArray())
        return source
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
            diagnostics = listOf(CompilerDiagnostic("ERROR", "timeout", null))
        )
    }

    private fun diagnostics(stderr: String): List<CompilerDiagnostic> {
        return stderr.lines()
            .filter { it.contains("error", ignoreCase = true) || it.contains("warning", ignoreCase = true) }
            .map {
                val severity = if (it.contains("warning", ignoreCase = true)) "WARNING" else "ERROR"
                CompilerDiagnostic(severity, it.trim(), null)
            }
    }
}
