package kai.domain.testcase

enum class CompilationTarget {
    JVM,
    JS,
    NATIVE,
    WASM
}

@JvmInline
value class LanguageVersion(val value: String) {
    init {
        require(value.isNotBlank()) { "languageVersion must not be blank" }
    }

    companion object {
        val DEFAULT = LanguageVersion("default")
    }
}

@JvmInline
value class ApiVersion(val value: String) {
    init {
        require(value.isNotBlank()) { "apiVersion must not be blank" }
    }

    companion object {
        val DEFAULT = ApiVersion("default")
    }
}
