package kai.storage.fs

class JsonParser(private val source: String) {
    private var index = 0

    fun parse(): JsonValue {
        skipWhitespace()
        val value = parseValue()
        skipWhitespace()
        require(index == source.length) { "Unexpected trailing content" }
        return value
    }

    private fun parseValue(): JsonValue {
        skipWhitespace()
        return when (peek()) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> JsonValue.Str(parseString())
            't' -> parseLiteral("true", JsonValue.Bool(true))
            'f' -> parseLiteral("false", JsonValue.Bool(false))
            'n' -> parseLiteral("null", JsonValue.Nil)
            else -> JsonValue.Num(parseNumber())
        }
    }

    private fun parseObject(): JsonValue.Obj {
        consume('{')
        val values = linkedMapOf<String, JsonValue>()
        skipWhitespace()
        if (peek() == '}') {
            consume('}')
            return JsonValue.Obj(values)
        }
        while (true) {
            val key = parseString()
            consume(':')
            values[key] = parseValue()
            skipWhitespace()
            if (peek() == '}') {
                consume('}')
                return JsonValue.Obj(values)
            }
            consume(',')
        }
    }

    private fun parseArray(): JsonValue.Arr {
        consume('[')
        val values = mutableListOf<JsonValue>()
        skipWhitespace()
        if (peek() == ']') {
            consume(']')
            return JsonValue.Arr(values)
        }
        while (true) {
            values += parseValue()
            skipWhitespace()
            if (peek() == ']') {
                consume(']')
                return JsonValue.Arr(values)
            }
            consume(',')
        }
    }

    private fun parseString(): String {
        consume('"')
        val result = StringBuilder()
        while (index < source.length) {
            val current = source[index++]
            if (current == '"') {
                return result.toString()
            }
            if (current == '\\') {
                val escaped = source[index++]
                result.append(
                    when (escaped) {
                        'n' -> '\n'
                        '\\' -> '\\'
                        '"' -> '"'
                        else -> escaped
                    }
                )
            } else {
                result.append(current)
            }
        }
        error("Unterminated string")
    }

    private fun parseNumber(): Long {
        val start = index
        while (index < source.length && isNumberChar(source[index])) {
            index++
        }
        return source.substring(start, index).toLong()
    }

    private fun isNumberChar(char: Char): Boolean {
        return char == '-' || char.isDigit()
    }

    private fun parseLiteral(text: String, value: JsonValue): JsonValue {
        require(source.startsWith(text, index)) { "Expected $text" }
        index += text.length
        return value
    }

    private fun consume(expected: Char) {
        skipWhitespace()
        require(source[index] == expected) { "Expected $expected at position $index" }
        index++
    }

    private fun peek(): Char {
        skipWhitespace()
        return source[index]
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index].isWhitespace()) {
            index++
        }
    }
}
