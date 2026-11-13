package fr.adrienbrault.idea.symfony2plugin.tests.translation.icu.lang

import com.intellij.lexer.Lexer
import com.intellij.testFramework.LexerTestCase
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageLexer

class IcuMessageLexerTest : LexerTestCase() {
    override fun createLexer(): Lexer = IcuMessageLexer()

    override fun getDirPath(): String = ""

    fun testSimpleArguments() {
        doTest(
            "{sum, number} on {label}",
            """
            LBRACE ('{')
            ARGUMENT_NAME ('sum')
            COMMA (',')
            WHITE_SPACE (' ')
            ARGUMENT_TYPE ('number')
            RBRACE ('}')
            TEXT (' on ')
            LBRACE ('{')
            ARGUMENT_NAME ('label')
            RBRACE ('}')
            """.trimIndent(),
        )
    }

    fun testPluralWithOffsetSelectorsHashAndNestedArgument() {
        doTest(
            "{n, plural, offset:1 =0 {none} one {# by {author}} other {#}}",
            """
            LBRACE ('{')
            ARGUMENT_NAME ('n')
            COMMA (',')
            WHITE_SPACE (' ')
            ARGUMENT_TYPE ('plural')
            COMMA (',')
            WHITE_SPACE (' ')
            OFFSET ('offset:')
            NUMBER ('1')
            WHITE_SPACE (' ')
            SELECTOR ('=0')
            WHITE_SPACE (' ')
            LBRACE ('{')
            TEXT ('none')
            RBRACE ('}')
            WHITE_SPACE (' ')
            SELECTOR ('one')
            WHITE_SPACE (' ')
            LBRACE ('{')
            HASH ('#')
            TEXT (' by ')
            LBRACE ('{')
            ARGUMENT_NAME ('author')
            RBRACE ('}')
            RBRACE ('}')
            WHITE_SPACE (' ')
            SELECTOR ('other')
            WHITE_SPACE (' ')
            LBRACE ('{')
            HASH ('#')
            RBRACE ('}')
            RBRACE ('}')
            """.trimIndent(),
        )
    }

    fun testHashIsTextOutsidePlural() {
        doTest(
            "# {g, select, other {#}}",
            """
            TEXT ('# ')
            LBRACE ('{')
            ARGUMENT_NAME ('g')
            COMMA (',')
            WHITE_SPACE (' ')
            ARGUMENT_TYPE ('select')
            COMMA (',')
            WHITE_SPACE (' ')
            SELECTOR ('other')
            WHITE_SPACE (' ')
            LBRACE ('{')
            TEXT ('#')
            RBRACE ('}')
            RBRACE ('}')
            """.trimIndent(),
        )
    }

    fun testArgumentStyles() {
        doTest(
            "{p, number, ::currency/EUR} {d, date, short } {c, choice, 0#none|1#{one}}",
            """
            LBRACE ('{')
            ARGUMENT_NAME ('p')
            COMMA (',')
            WHITE_SPACE (' ')
            ARGUMENT_TYPE ('number')
            COMMA (',')
            WHITE_SPACE (' ')
            ARGUMENT_STYLE ('::currency/EUR')
            RBRACE ('}')
            TEXT (' ')
            LBRACE ('{')
            ARGUMENT_NAME ('d')
            COMMA (',')
            WHITE_SPACE (' ')
            ARGUMENT_TYPE ('date')
            COMMA (',')
            WHITE_SPACE (' ')
            ARGUMENT_STYLE ('short')
            WHITE_SPACE (' ')
            RBRACE ('}')
            TEXT (' ')
            LBRACE ('{')
            ARGUMENT_NAME ('c')
            COMMA (',')
            WHITE_SPACE (' ')
            ARGUMENT_TYPE ('choice')
            COMMA (',')
            WHITE_SPACE (' ')
            ARGUMENT_STYLE ('0#none|1#{one}')
            RBRACE ('}')
            """.trimIndent(),
        )
    }

    fun testApostrophes() {
        doTest(
            "I''m '{literal}' it's {n, plural, other {'#' '{x''y}'}}",
            """
            TEXT ('I')
            ESCAPED_QUOTE ('''')
            TEXT ('m ')
            QUOTED_TEXT (''{literal}'')
            TEXT (' it's ')
            LBRACE ('{')
            ARGUMENT_NAME ('n')
            COMMA (',')
            WHITE_SPACE (' ')
            ARGUMENT_TYPE ('plural')
            COMMA (',')
            WHITE_SPACE (' ')
            SELECTOR ('other')
            WHITE_SPACE (' ')
            LBRACE ('{')
            QUOTED_TEXT (''#'')
            TEXT (' ')
            QUOTED_TEXT (''{x''y}'')
            RBRACE ('}')
            RBRACE ('}')
            """.trimIndent(),
        )
    }

    fun testInvalidInput() {
        doTest(
            "a} {user.name} {n plural} {x, number, 'abc}",
            """
            TEXT ('a')
            RBRACE ('}')
            TEXT (' ')
            LBRACE ('{')
            ARGUMENT_NAME ('user')
            BAD_CHARACTER ('.')
            ARGUMENT_NAME ('name')
            RBRACE ('}')
            TEXT (' ')
            LBRACE ('{')
            ARGUMENT_NAME ('n')
            WHITE_SPACE (' ')
            ARGUMENT_NAME ('plural')
            RBRACE ('}')
            TEXT (' ')
            LBRACE ('{')
            ARGUMENT_NAME ('x')
            COMMA (',')
            WHITE_SPACE (' ')
            ARGUMENT_TYPE ('number')
            COMMA (',')
            WHITE_SPACE (' ')
            UNCLOSED_QUOTE (''abc}')
            """.trimIndent(),
        )
    }
}
