package kai.domain.execution

import kai.domain.testcase.CompilationTarget

data class ExecutionConfig(
    val timeoutMillis: Long,
    val environment: Map<String, String>,
    val captureArtifacts: Boolean
) {
    companion object {
        fun create(
            timeoutMillis: Long,
            environment: Map<String, String> = emptyMap(),
            captureArtifacts: Boolean = false
        ): ExecutionConfig {
            require(timeoutMillis > 0) { "timeoutMillis must be positive" }
            return ExecutionConfig(timeoutMillis, environment.toMap(), captureArtifacts)
        }
    }
}

data class ExecutorCapabilities(
    val supportsJvm: Boolean,
    val supportsJs: Boolean,
    val supportsNative: Boolean,
    val supportsWasm: Boolean
) {
    fun supports(target: CompilationTarget): Boolean {
        return when (target) {
            CompilationTarget.JVM -> supportsJvm
            CompilationTarget.JS -> supportsJs
            CompilationTarget.NATIVE -> supportsNative
            CompilationTarget.WASM -> supportsWasm
        }
    }

    companion object {
        fun jvmOnly(): ExecutorCapabilities {
            return ExecutorCapabilities(true, false, false, false)
        }
    }
}

sealed class ExecutorProbeResult {
    data class Available(val message: String) : ExecutorProbeResult()
    data class Unavailable(val message: String) : ExecutorProbeResult()
}
