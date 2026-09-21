package com.prime.nobuffer.json

/**
 * Minimal JSON parser/serializer for JVM unit tests and DataStore payloads.
 * Supports objects, arrays, strings, numbers, booleans, and null.
 */
object JsonLite {

    fun parse(text: String): Any? {
        val parser = Parser(text)
        val value = parser.parseValue()
        parser.expectEnd()
        return value
    }

    fun stringify(value: Any?): String = buildString { write(value) }

    private class Parser(private val s: String) {
        private var i = 0

        fun expectEnd() {
            skipWs()
            if (i < s.length) error("trailing JSON at $i")
        }

        fun parseValue(): Any? {
            skipWs()
            if (i >= s.length) error("unexpected end of JSON")
            return when (val c = s[i]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> { consume("true"); true }
                'f' -> { consume("false"); false }
                'n' -> { consume("null"); null }
                else -> if (c == '-' || c in '0'..'9') parseNumber() else error("unexpected '$c' at $i")
            }
        }

        private fun parseObject(): Map<String, Any?> {
            consume('{')
            val out = linkedMapOf<String, Any?>()
            skipWs()
            if (peek('}')) {
                consume('}')
                return out
            }
            while (true) {
                skipWs()
                val key = parseString()
                skipWs()
                consume(':')
                out[key] = parseValue()
                skipWs()
                when {
                    peek(',') -> {
                        consume(',')
                    }
                    peek('}') -> {
                        consume('}')
                        return out
                    }
                    else -> error("expected ',' or '}' at $i")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            consume('[')
            val out = mutableListOf<Any?>()
            skipWs()
            if (peek(']')) {
                consume(']')
                return out
            }
            while (true) {
                out += parseValue()
                skipWs()
                when {
                    peek(',') -> {
                        consume(',')
                    }
                    peek(']') -> {
                        consume(']')
                        return out
                    }
                    else -> error("expected ',' or ']' at $i")
                }
            }
        }

        private fun parseString(): String {
            consume('"')
            val sb = StringBuilder()
            while (i < s.length) {
                when (val c = s[i++]) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (i >= s.length) error("unterminated escape")
                        when (val e = s[i++]) {
                            '"', '\\', '/' -> sb.append(e)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) error("bad unicode escape")
                                val hex = s.substring(i, i + 4)
                                sb.append(hex.toInt(16).toChar())
                                i += 4
                            }
                            else -> error("bad escape '\\$e'")
                        }
                    }
                    else -> sb.append(c)
                }
            }
            error("unterminated string")
        }

        private fun parseNumber(): Number {
            val start = i
            if (peek('-')) i++
            if (peek('0')) {
                i++
            } else {
                if (i >= s.length || s[i] !in '1'..'9') error("bad number at $start")
                while (i < s.length && s[i] in '0'..'9') i++
            }
            var isFloat = false
            if (peek('.')) {
                isFloat = true
                i++
                if (i >= s.length || s[i] !in '0'..'9') error("bad fraction")
                while (i < s.length && s[i] in '0'..'9') i++
            }
            if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
                isFloat = true
                i++
                if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
                if (i >= s.length || s[i] !in '0'..'9') error("bad exponent")
                while (i < s.length && s[i] in '0'..'9') i++
            }
            val raw = s.substring(start, i)
            return if (isFloat) raw.toDouble() else raw.toLong()
        }

        private fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        private fun peek(c: Char): Boolean = i < s.length && s[i] == c

        private fun consume(c: Char) {
            skipWs()
            if (i >= s.length || s[i] != c) error("expected '$c' at $i")
            i++
        }

        private fun consume(token: String) {
            skipWs()
            if (!s.startsWith(token, i)) error("expected '$token' at $i")
            i += token.length
        }
    }

    private fun StringBuilder.write(value: Any?) {
        when (value) {
            null -> append("null")
            is Boolean -> append(value)
            is Int, is Long, is Short, is Byte -> append(value.toString())
            is Float, is Double -> append(value.toString())
            is Number -> append(value.toString())
            is String -> writeString(value)
            is Map<*, *> -> {
                append('{')
                var first = true
                for ((k, v) in value) {
                    if (k !is String) continue
                    if (!first) append(',')
                    first = false
                    writeString(k)
                    append(':')
                    write(v)
                }
                append('}')
            }
            is Iterable<*> -> {
                append('[')
                var first = true
                for (item in value) {
                    if (!first) append(',')
                    first = false
                    write(item)
                }
                append(']')
            }
            is Array<*> -> write(value.asList())
            else -> writeString(value.toString())
        }
    }

    private fun StringBuilder.writeString(value: String) {
        append('"')
        value.forEach { c ->
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') {
                    append("\\u")
                    append(c.code.toString(16).padStart(4, '0'))
                } else {
                    append(c)
                }
            }
        }
        append('"')
    }
}
