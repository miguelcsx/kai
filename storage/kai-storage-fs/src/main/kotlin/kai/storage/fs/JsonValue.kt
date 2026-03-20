package kai.storage.fs

sealed class JsonValue {
    data class Obj(val values: Map<String, JsonValue>) : JsonValue()
    data class Arr(val values: List<JsonValue>) : JsonValue()
    data class Str(val value: String) : JsonValue()
    data class Num(val value: Long) : JsonValue()
    data class Bool(val value: Boolean) : JsonValue()
    object Nil : JsonValue()
}
