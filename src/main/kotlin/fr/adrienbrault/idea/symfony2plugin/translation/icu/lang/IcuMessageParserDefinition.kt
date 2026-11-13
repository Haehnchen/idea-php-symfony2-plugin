package fr.adrienbrault.idea.symfony2plugin.translation.icu.lang

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageElementTypes.ARGUMENT
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageElementTypes.OPTION
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageElementTypes.SUB_MESSAGE
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.ARGUMENT_NAME
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.ARGUMENT_STYLE
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.ARGUMENT_TYPE
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.COMMA
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.LBRACE
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.NUMBER
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.OFFSET
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.RBRACE
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.SELECTOR
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes.UNCLOSED_QUOTE

class IcuMessageParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = IcuMessageLexer()

    override fun createParser(project: Project?): PsiParser = IcuMessagePsiParser()

    override fun getFileNodeType(): IFileElementType = IcuMessageElementTypes.FILE

    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = IcuMessageFile(viewProvider)
}

class IcuMessagePsiParser : PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val file = builder.mark()
        Parsing(builder).parseMessage(topLevel = true)
        file.done(root)

        return builder.treeBuilt
    }
}

private class Parsing(private val builder: PsiBuilder) {
    // a sub-message stops on the "}" closing it, without reading it
    fun parseMessage(topLevel: Boolean) {
        while (!builder.eof()) {
            when (builder.tokenType) {
                LBRACE -> parseArgument()
                RBRACE -> if (topLevel) errorOnToken("Unmatched '}'") else return
                else -> builder.advanceLexer()
            }
        }
    }

    private fun parseArgument() {
        val argument = builder.mark()
        builder.advanceLexer()
        parseArgumentContent()
        argument.done(ARGUMENT)
    }

    // reads the argument up to its closing "}", or reports the first error and skips to it
    private fun parseArgumentContent() {
        if (!consume(ARGUMENT_NAME)) {
            return unexpected(if (builder.tokenType == TokenType.BAD_CHARACTER) "Invalid argument name" else "Argument name expected")
        }

        when (builder.tokenType) {
            RBRACE -> return builder.advanceLexer()
            COMMA -> builder.advanceLexer()
            TokenType.BAD_CHARACTER -> return unexpected("Invalid argument name")
            else -> return unexpected("',' or '}' expected")
        }

        val type = builder.tokenText?.lowercase()
        if (!consume(ARGUMENT_TYPE)) {
            return unexpected("Argument type expected")
        }

        val complex = type in IcuMessageSyntax.COMPLEX_TYPES
        when (builder.tokenType) {
            RBRACE -> return if (complex) unexpected("',' expected") else builder.advanceLexer()
            COMMA -> builder.advanceLexer()
            else -> return unexpected("',' or '}' expected")
        }

        if (complex) {
            parseOptions()
        } else {
            consume(ARGUMENT_STYLE)
            if (builder.tokenType == UNCLOSED_QUOTE) {
                return errorOnToken("Unclosed quote in argument style")
            }
        }

        if (!consume(RBRACE)) {
            unexpected("'}' expected")
        }
    }

    private fun parseOptions() {
        if (consume(OFFSET) && !consume(NUMBER)) {
            builder.error("Offset value expected")
        }

        var options = 0
        while (!builder.eof() && builder.tokenType != RBRACE) {
            if (builder.tokenType != SELECTOR) {
                errorOnToken("Option selector expected")
                continue
            }

            val option = builder.mark()
            builder.advanceLexer()
            if (!consume(LBRACE)) {
                option.done(OPTION)
                if (builder.eof() || builder.tokenType == RBRACE) builder.error("'{' expected") else errorOnToken("'{' expected")
                continue
            }

            val subMessage = builder.mark()
            parseMessage(topLevel = false)
            subMessage.done(SUB_MESSAGE)

            // at the end of the message the missing "}" is reported once, for the argument
            consume(RBRACE)
            option.done(OPTION)
            options++
        }

        if (options == 0 && !builder.eof()) {
            builder.error("Option expected")
        }
    }

    // an unexpected "}" still closes the argument; any other token is marked and skipped up to the closing "}"
    private fun unexpected(message: String) {
        when {
            builder.eof() -> builder.error("'}' expected")
            builder.tokenType == RBRACE -> {
                builder.error(message)
                builder.advanceLexer()
            }
            else -> {
                errorOnToken(message)
                skipToArgumentEnd()
            }
        }
    }

    private fun skipToArgumentEnd() {
        var depth = 0
        while (!builder.eof()) {
            when (builder.tokenType) {
                LBRACE -> depth++
                RBRACE -> if (depth-- == 0) {
                    builder.advanceLexer()
                    return
                }
            }

            builder.advanceLexer()
        }
    }

    private fun errorOnToken(message: String) {
        val marker = builder.mark()
        builder.advanceLexer()
        marker.error(message)
    }

    private fun consume(tokenType: IElementType): Boolean {
        if (builder.tokenType != tokenType) {
            return false
        }

        builder.advanceLexer()
        return true
    }
}
