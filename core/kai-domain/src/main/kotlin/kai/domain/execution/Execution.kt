package kai.domain.execution

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
