package kai.domain.observations

import kai.domain.id.TestCaseId

data class CompilerDiagnostic(
    val severity: DiagnosticSeverity,
    val message: String,
    val location: String?
)

data class CompilationArtifact(
    val kind: ArtifactKind,
    val path: String,
    val sizeBytes: Long
)

data class ExecutionResult(
    val profileName: String,
    val command: List<String>,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val diagnostics: List<CompilerDiagnostic>,
    val artifacts: List<CompilationArtifact>
) {
    companion object {
        fun create(
            profileName: String,
            command: List<String>,
            exitCode: Int,
            stdout: String,
            stderr: String,
            durationMs: Long,
            diagnostics: List<CompilerDiagnostic> = emptyList(),
            artifacts: List<CompilationArtifact> = emptyList()
        ): ExecutionResult {
            return ExecutionResult(
                profileName = profileName,
                command = command.toList(),
                exitCode = exitCode,
                stdout = stdout,
                stderr = stderr,
                durationMs = durationMs,
                diagnostics = diagnostics.toList(),
                artifacts = artifacts.toList()
            )
        }
    }
}

data class Observations(
    val testCaseId: TestCaseId,
    val results: List<ExecutionResult>
) {
    companion object {
        fun create(
            testCaseId: TestCaseId,
            results: List<ExecutionResult>
        ): Observations {
            require(results.isNotEmpty()) { "results must not be empty" }
            return Observations(testCaseId, results.toList())
        }
    }
}
