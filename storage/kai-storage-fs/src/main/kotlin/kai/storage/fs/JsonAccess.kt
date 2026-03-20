package kai.storage.fs

internal fun JsonValue.asObject(): JsonValue.Obj {
    return this as JsonValue.Obj
}

internal fun JsonValue.asArray(): List<JsonValue> {
    return (this as JsonValue.Arr).values
}

internal fun JsonValue.asString(): String {
    return (this as JsonValue.Str).value
}

internal fun JsonValue.asLong(): Long {
    return (this as JsonValue.Num).value
}

internal fun JsonValue.get(name: String): JsonValue {
    return asObject().values.getValue(name)
}

internal fun JsonValue.string(name: String): String {
    return get(name).asString()
}

internal fun JsonValue.long(name: String): Long {
    return get(name).asLong()
}

internal fun JsonValue.array(name: String): List<JsonValue> {
    return get(name).asArray()
}

internal fun JsonValue.obj(name: String): JsonValue.Obj {
    return get(name).asObject()
}

internal fun JsonValue.optString(name: String): String? {
    val value = asObject().values[name] ?: return null
    return if (value is JsonValue.Nil) null else value.asString()
}
