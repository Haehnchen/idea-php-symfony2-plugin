package fr.adrienbrault.idea.symfony2plugin.translation.icu

/**
 * A parameter of a translation message. [type] is "string", "number" or "date" for ICU arguments and null for
 * other placeholders such as "%name%"; [values] are the options of an ICU "select".
 */
data class TranslationParameter @JvmOverloads constructor(val name: String, val type: String? = null, val values: List<String> = emptyList())

/**
 * Reads the arguments of an ICU MessageFormat message, following the grammar of ICU4J's MessagePattern
 * with its default apostrophe mode.
 */
object IcuMessageParser {
    private val SIMPLE_ARGUMENT_TYPES = mapOf(
        "number" to "number",
        "spellout" to "number",
        "ordinal" to "number",
        "duration" to "number",
        "choice" to "number",
        "date" to "date",
        "time" to "date",
    )

    /**
     * Arguments by name in order of appearance, or null when the message is not valid ICU MessageFormat.
     */
    @JvmStatic
    fun getParameters(message: String): Map<String, TranslationParameter>? {
        val arguments = try {
            Parser(message).parse()
        } catch (_: IcuSyntaxException) {
            return null
        }

        return buildMap { arguments.forEach { putIfAbsent(it.name, it) } }
    }

    private class IcuSyntaxException : RuntimeException()

    private class Parser(private val message: String) {
        private var offset = 0
        private val arguments = mutableListOf<TranslationParameter>()

        fun parse(): List<TranslationParameter> {
            parseMessage(inPlural = false)
            if (offset < message.length) {
                fail()
            }

            return arguments
        }

        // stops on the "}" closing a sub-message, without reading it
        private fun parseMessage(inPlural: Boolean) {
            while (offset < message.length) {
                when (message[offset]) {
                    '\'' -> skipApostrophe(inPlural)
                    '{' -> parseArgument()
                    '}' -> return
                    else -> offset++
                }
            }
        }

        private fun skipApostrophe(inPlural: Boolean) {
            val next = message.getOrNull(offset + 1)
            if (next != '{' && next != '}' && !(inPlural && next == '#')) {
                offset += if (next == '\'') 2 else 1
                return
            }

            // quoted literal text runs to the next single apostrophe, or to the end of the message
            offset++
            while (offset < message.length) {
                if (message[offset++] == '\'') {
                    if (message.getOrNull(offset) != '\'') {
                        return
                    }

                    offset++
                }
            }
        }

        private fun parseArgument() {
            offset++
            val name = readIdentifier()
            if (peek() == '}') {
                offset++
                arguments.add(TranslationParameter(name, "string"))
                return
            }

            expect(',')
            when (val type = readIdentifier().lowercase()) {
                "plural", "selectordinal" -> {
                    arguments.add(TranslationParameter(name, "number"))
                    expect(',')
                    parseOptions(inPlural = true)
                }
                "select" -> {
                    val index = arguments.size
                    arguments.add(TranslationParameter(name, "string"))
                    expect(',')
                    arguments[index] = TranslationParameter(name, "string", parseOptions(inPlural = false))
                }
                else -> {
                    arguments.add(TranslationParameter(name, SIMPLE_ARGUMENT_TYPES[type] ?: "string"))
                    if (peek() == ',') {
                        offset++
                        skipStyle()
                    }

                    expect('}')
                }
            }
        }

        // reads "selector {message}" pairs and the "}" closing the argument
        private fun parseOptions(inPlural: Boolean): List<String> {
            val selectors = mutableListOf<String>()
            while (peek() != '}') {
                if (inPlural && selectors.isEmpty() && message.startsWith("offset:", offset)) {
                    offset += "offset:".length
                    skipWhitespace()
                    readNumber()
                    continue
                }

                selectors += if (peek() == '=') {
                    offset++
                    "=" + readNumber()
                } else {
                    readIdentifier()
                }

                expect('{')
                parseMessage(inPlural)
                expect('}')
            }

            if (selectors.isEmpty()) {
                fail()
            }

            offset++
            return selectors
        }

        // a simple argument style may hold balanced braces and quoted text, e.g. "::currency/EUR" or "0#none|1#one"
        private fun skipStyle() {
            var depth = 0
            while (offset < message.length) {
                when (message[offset]) {
                    '\'' -> {
                        offset = message.indexOf('\'', offset + 1)
                        if (offset < 0) {
                            fail()
                        }
                    }
                    '{' -> depth++
                    '}' -> if (depth-- == 0) return
                }

                offset++
            }
        }

        private fun readIdentifier(): String {
            skipWhitespace()
            val start = offset
            while (offset < message.length && (message[offset].isLetterOrDigit() || message[offset] == '_')) {
                offset++
            }

            return message.substring(start, offset).ifEmpty { fail() }
        }

        private fun readNumber(): String {
            val start = offset
            while (offset < message.length && (message[offset].isDigit() || message[offset] in ".-+")) {
                offset++
            }

            return message.substring(start, offset).ifEmpty { fail() }
        }

        private fun expect(char: Char) {
            if (peek() != char) {
                fail()
            }

            offset++
        }

        private fun peek(): Char? {
            skipWhitespace()
            return message.getOrNull(offset)
        }

        private fun skipWhitespace() {
            while (offset < message.length && message[offset].isWhitespace()) {
                offset++
            }
        }

        private fun fail(): Nothing = throw IcuSyntaxException()
    }
}
