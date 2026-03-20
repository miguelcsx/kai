package kai.storage.fs

object JsonWriter {
    fun write(value: JsonValue): String {
        return when (value) {
            is JsonValue.Obj -> writeObject(value)
            is JsonValue.Arr -> writeArray(value)
            is JsonValue.Str -> "\"${escape(value.value)}\""
            is JsonValue.Num -> value.value.toString()
            is JsonValue.Bool -> value.value.toString()
            JsonValue.Nil -> "null"
        }
    }

    private fun writeObject(value: JsonValue.Obj): String {
        return value.values.entries.joinToString(prefix = "{", postfix = "}") {
            "\"${escape(it.key)}\":${write(it.value)}"
        }
    }

    private fun writeArray(value: JsonValue.Arr): String {
        return value.values.joinToString(prefix = "[", postfix = "]") { write(it) }
    }

    private fun escape(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
    }
}
