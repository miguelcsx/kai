package kai.domain.observations

enum class DiagnosticSeverity {
    ERROR,
    WARNING,
    INFO
}

enum class ArtifactKind {
    CLASSFILE,
    JS,
    NATIVE_BINARY,
    IR
}
