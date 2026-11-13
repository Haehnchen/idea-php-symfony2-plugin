package fr.adrienbrault.idea.symfony2plugin.translation.icu.lang

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.ARGUMENT_NAME
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.ARGUMENT_STYLE
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.ARGUMENT_TYPE
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.COMMA
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.ESCAPED_QUOTE
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.HASH
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.LBRACE
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.NUMBER
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.OFFSET
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.QUOTED_TEXT
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.RBRACE
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.SELECTOR
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.TEXT
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.UNCLOSED_QUOTE

/**
 * Tokenizes an ICU MessageFormat message following the grammar of ICU4J's MessagePattern with its default apostrophe mode.
 * Messages are short, so the buffer is tokenized at once; only tokens outside any argument are in the restartable state 0.
 */
class IcuMessageLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var bufferEnd = 0
    private var tokens: List<Token> = emptyList()
    private var index = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        this.tokens = Tokenizer(buffer, startOffset, endOffset).tokenize()
        this.index = 0
    }

    override fun getState(): Int = tokens.getOrNull(index)?.state ?: 0

    override fun getTokenType(): IElementType? = tokens.getOrNull(index)?.type

    override fun getTokenStart(): Int = tokens.getOrNull(index)?.start ?: bufferEnd

    override fun getTokenEnd(): Int = tokens.getOrNull(index)?.end ?: bufferEnd

    override fun advance() {
        index++
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = bufferEnd
}

private class Token(val type: IElementType, val start: Int, val end: Int, val state: Int)

private enum class Phase { NAME, TYPE, STYLE, OPTIONS }

private sealed interface Frame

private class MessageFrame(val inPlural: Boolean) : Frame

private class ArgumentFrame(var phase: Phase = Phase.NAME, var type: String? = null) : Frame

private class Tokenizer(private val text: CharSequence, private var offset: Int, private val end: Int) {
    private val tokens = mutableListOf<Token>()
    private val frames = ArrayDeque<Frame>(listOf(MessageFrame(inPlural = false)))

    fun tokenize(): List<Token> {
        while (offset < end) {
            when (val frame = frames.last()) {
                is MessageFrame -> lexMessage(frame)
                is ArgumentFrame -> lexArgument(frame)
            }
        }

        return tokens
    }

    private fun lexMessage(frame: MessageFrame) {
        val char = text[offset]
        when {
            char == '{' -> {
                emit(LBRACE)
                frames.addLast(ArgumentFrame())
            }
            char == '}' -> {
                emit(RBRACE)
                // a "}" at the top level closes nothing and is reported by the parser
                if (frames.size > 1) {
                    frames.removeLast()
                }
            }
            char == '#' && frame.inPlural -> emit(HASH)
            char == '\'' && charAt(offset + 1) == '\'' -> emit(ESCAPED_QUOTE, offset + 2)
            startsQuote(offset, frame) -> emit(QUOTED_TEXT, quotedTextEnd(offset + 1))
            else -> emit(TEXT, textEnd(frame))
        }
    }

    private fun lexArgument(frame: ArgumentFrame) {
        val char = text[offset]
        if (char.isWhitespace()) {
            emit(TokenType.WHITE_SPACE, scanWhile(offset, Char::isWhitespace))
            return
        }

        if (char == '}') {
            emit(RBRACE)
            frames.removeLast()
            return
        }

        when (frame.phase) {
            Phase.NAME -> when {
                isIdentifierChar(char) -> emit(ARGUMENT_NAME, identifierEnd(offset))
                char == ',' -> {
                    emit(COMMA)
                    frame.phase = Phase.TYPE
                }
                else -> emit(TokenType.BAD_CHARACTER)
            }
            Phase.TYPE -> when {
                isIdentifierChar(char) -> {
                    val typeEnd = identifierEnd(offset)
                    frame.type = text.substring(offset, typeEnd).lowercase()
                    emit(ARGUMENT_TYPE, typeEnd)
                }
                char == ',' -> {
                    emit(COMMA)
                    frame.phase = if (frame.type in IcuMessageSyntax.COMPLEX_TYPES) Phase.OPTIONS else Phase.STYLE
                }
                else -> emit(TokenType.BAD_CHARACTER)
            }
            Phase.STYLE -> lexStyle()
            Phase.OPTIONS -> when {
                char == '{' -> {
                    emit(LBRACE)
                    frames.addLast(MessageFrame(inPlural = frame.type in IcuMessageSyntax.PLURAL_TYPES))
                }
                char == '=' -> emit(SELECTOR, numberEnd(offset + 1))
                char.isDigit() || char == '-' -> emit(NUMBER, numberEnd(offset))
                isIdentifierChar(char) -> {
                    val selectorEnd = identifierEnd(offset)
                    val isOffset = frame.type in IcuMessageSyntax.PLURAL_TYPES && text.substring(offset, selectorEnd) == "offset" && charAt(selectorEnd) == ':'
                    if (isOffset) emit(OFFSET, selectorEnd + 1) else emit(SELECTOR, selectorEnd)
                }
                else -> emit(TokenType.BAD_CHARACTER)
            }
        }
    }

    // a simple style runs to the "}" closing the argument and may hold balanced braces and quoted text
    private fun lexStyle() {
        var depth = 0
        var index = offset
        while (index < end) {
            when (text[index]) {
                '\'' -> {
                    val closingQuote = scanWhile(index + 1) { it != '\'' }
                    if (closingQuote == end) {
                        emitStyle(index)
                        emit(UNCLOSED_QUOTE, end)
                        return
                    }

                    index = closingQuote
                }
                '{' -> depth++
                '}' -> if (depth-- == 0) break
            }

            index++
        }

        emitStyle(index)
    }

    private fun emitStyle(styleEnd: Int) {
        var textEnd = styleEnd
        while (textEnd > offset && text[textEnd - 1].isWhitespace()) {
            textEnd--
        }

        if (textEnd > offset) {
            emit(ARGUMENT_STYLE, textEnd)
        }

        if (offset < styleEnd) {
            emit(TokenType.WHITE_SPACE, styleEnd)
        }
    }

    // an apostrophe starts quoted text only before a syntax character, see MessagePattern.ApostropheMode.DOUBLE_OPTIONAL
    private fun startsQuote(index: Int, frame: MessageFrame): Boolean {
        val next = charAt(index + 1)
        return text[index] == '\'' && (next == '{' || next == '}' || (next == '#' && frame.inPlural))
    }

    // quoted text runs to the next single apostrophe, or to the end of the message
    private fun quotedTextEnd(from: Int): Int {
        var index = from
        while (index < end) {
            if (text[index] == '\'') {
                if (charAt(index + 1) != '\'') {
                    return index + 1
                }

                index++
            }

            index++
        }

        return end
    }

    private fun textEnd(frame: MessageFrame): Int {
        var index = offset
        while (index < end) {
            val char = text[index]
            val isSyntax = char == '{' || char == '}' || (char == '#' && frame.inPlural) ||
                (char == '\'' && (charAt(index + 1) == '\'' || startsQuote(index, frame)))
            if (isSyntax && index > offset) {
                break
            }

            index++
        }

        return index
    }

    private fun identifierEnd(from: Int): Int = scanWhile(from, ::isIdentifierChar)

    private fun numberEnd(from: Int): Int = scanWhile(from) { it.isDigit() || it in ".-+" }

    private inline fun scanWhile(from: Int, predicate: (Char) -> Boolean): Int {
        var index = from
        while (index < end && predicate(text[index])) {
            index++
        }

        return index
    }

    private fun isIdentifierChar(char: Char): Boolean = char.isLetterOrDigit() || char == '_'

    private fun charAt(index: Int): Char = if (index < end) text[index] else Char.MIN_VALUE

    // the state is taken before the token changes the frames, so only tokens outside any argument are restartable
    private fun emit(type: IElementType, tokenEnd: Int = offset + 1) {
        tokens.add(Token(type, offset, tokenEnd, if (frames.size == 1) 0 else 1))
        offset = tokenEnd
    }
}
