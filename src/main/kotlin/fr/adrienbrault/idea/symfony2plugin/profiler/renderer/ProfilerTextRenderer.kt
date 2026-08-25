package fr.adrienbrault.idea.symfony2plugin.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpIntegerKey
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerBoolean
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerEnum
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerFloat
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerInteger
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerNull
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerOpaque
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerReference
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerValue

/** Renders collector-neutral values as bounded indented key/value text with recognized lists. */
internal object ProfilerTextRenderer {
    private const val MAX_DEPTH = 32
    private const val MAX_LINES = 10_000
    private const val MAX_KEY_LENGTH = 200
    private const val MAX_SCALAR_LENGTH = 1_000
    private val MARKDOWN_LEADING_CHARACTERS = setOf('#', '-', '*', '+', '>', '|', '`')
    private val CONTROL_CHARACTERS = Regex("[\\u0000-\\u001F\\u007F]+")
    private val BACKTICK_RUN = Regex("`+")

    fun render(value: ProfilerValue, initialIndent: Int = 0): List<String> {
        val state = RenderState()
        if (value is ProfilerArray) {
            if (value.entries.isEmpty()) {
                state.add(" ".repeat(initialIndent) + "(empty)")
            } else {
                state.appendArray(value, initialIndent, 0)
            }
        } else {
            state.add(" ".repeat(initialIndent) + "Value: " + scalar(value))
        }

        if (state.truncated) {
            if (state.lines.size == MAX_LINES) {
                state.lines.removeLast()
            }
            state.lines.add(" ".repeat(initialIndent) + "[output truncated]")
        }
        return state.lines
    }

    /** Renders untrusted single-line text as a Markdown code span. */
    fun inlineCode(value: String): String {
        val text = value.replace(CONTROL_CHARACTERS, " ")
        val longestDelimiter = BACKTICK_RUN.findAll(text).maxOfOrNull { it.value.length } ?: 0
        val delimiter = "`".repeat(longestDelimiter + 1)
        val padding = if (
            text.startsWith('`') || text.endsWith('`') || text.startsWith(' ') || text.endsWith(' ')
        ) {
            " "
        } else {
            ""
        }

        return "$delimiter$padding$text$padding$delimiter"
    }

    private class RenderState {
        val lines = mutableListOf<String>()
        var truncated = false

        fun appendArray(value: ProfilerArray, indent: Int, depth: Int) {
            if (depth >= MAX_DEPTH) {
                add(" ".repeat(indent) + "[maximum depth reached]")
                return
            }

            if (value.isList()) {
                appendList(value, indent, depth)
                return
            }

            value.entries.forEach { entry ->
                if (truncated) {
                    return
                }
                val prefix = " ".repeat(indent) + key(entry.key)
                val nested = entry.value as? ProfilerArray
                if (nested == null) {
                    add("$prefix: ${scalar(entry.value)}")
                } else if (nested.entries.isEmpty()) {
                    add("$prefix: (empty)")
                } else {
                    add("$prefix:")
                    appendArray(nested, indent + 2, depth + 1)
                }
            }
        }

        private fun appendList(value: ProfilerArray, indent: Int, depth: Int) {
            value.entries.forEach { entry ->
                if (truncated) {
                    return
                }
                val prefix = " ".repeat(indent) + "-"
                val nested = entry.value as? ProfilerArray
                if (nested == null) {
                    add("$prefix ${scalar(entry.value)}")
                } else if (nested.entries.isEmpty()) {
                    add("$prefix (empty)")
                } else {
                    add(prefix)
                    appendArray(nested, indent + 2, depth + 1)
                }
            }
        }

        fun add(line: String) {
            if (lines.size >= MAX_LINES) {
                truncated = true
                return
            }
            lines.add(line)
        }
    }

    private fun ProfilerArray.isList(): Boolean = entries.isNotEmpty() && entries.withIndex().all { (index, entry) ->
        (entry.key as? PhpIntegerKey)?.value == index.toLong()
    }

    private fun key(key: fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArrayKey): String = when (key) {
        is PhpIntegerKey -> "[${key.value}]"
        is PhpStringKey -> {
            val text = key.bytes.utf8StringOrNull() ?: return "[binary key, ${key.bytes.size} bytes]"
            val escaped = escape(text, MAX_KEY_LENGTH)
            // Prefix syntax-looking keys so untrusted values cannot create Markdown blocks.
            if (escaped.firstOrNull() in MARKDOWN_LEADING_CHARACTERS) "key $escaped" else escaped
        }
    }

    private fun scalar(value: ProfilerValue): String = when (value) {
        ProfilerNull -> "null"
        is ProfilerBoolean -> value.value.toString()
        is ProfilerInteger -> value.value.toString()
        is ProfilerFloat -> value.value.toString()
        is ProfilerString -> value.utf8StringOrNull()?.let { escape(it, MAX_SCALAR_LENGTH) }
            ?: "[binary string, ${value.bytes.size} bytes]"

        is ProfilerArray -> if (value.entries.isEmpty()) "(empty)" else "[nested value]"
        is ProfilerEnum -> {
            val enumName = value.enumName.utf8StringOrNull() ?: "binary enum"
            val caseName = value.caseName.utf8StringOrNull() ?: "binary case"
            escape("$enumName::$caseName", MAX_SCALAR_LENGTH)
        }

        is ProfilerReference -> "[reference to position ${value.position}]"
        is ProfilerOpaque -> buildString {
            append("[opaque ${escape(value.kind, MAX_KEY_LENGTH)}")
            value.payloadBytes?.let { append(", $it bytes") }
            append(']')
        }
    }

    /** Escapes line-breaking controls before truncation so one value always occupies one line. */
    private fun escape(value: String, maxLength: Int): String {
        val escaped = buildString(value.length) {
            value.forEach { character ->
                when (character) {
                    '\u0000' -> append("\\0")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (character.code < 0x20 || character.code == 0x7f) {
                        append("\\u")
                        append(character.code.toString(16).padStart(4, '0'))
                    } else {
                        append(character)
                    }
                }
            }
        }
        return if (escaped.length <= maxLength) escaped else escaped.take(maxLength) + "… [truncated]"
    }
}
