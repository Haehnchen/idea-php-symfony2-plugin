package fr.adrienbrault.idea.symfony2plugin.translation.icu.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import javax.swing.Icon

object IcuMessageHighlighterColors {
    @JvmField val BRACES = createTextAttributesKey("SYMFONY_ICU_BRACES", DefaultLanguageHighlighterColors.BRACES)
    @JvmField val COMMA = createTextAttributesKey("SYMFONY_ICU_COMMA", DefaultLanguageHighlighterColors.COMMA)
    @JvmField val ARGUMENT_NAME = createTextAttributesKey("SYMFONY_ICU_ARGUMENT_NAME", DefaultLanguageHighlighterColors.PARAMETER)
    @JvmField val ARGUMENT_TYPE = createTextAttributesKey("SYMFONY_ICU_ARGUMENT_TYPE", DefaultLanguageHighlighterColors.KEYWORD)
    @JvmField val ARGUMENT_STYLE = createTextAttributesKey("SYMFONY_ICU_ARGUMENT_STYLE", DefaultLanguageHighlighterColors.CONSTANT)
    @JvmField val SELECTOR = createTextAttributesKey("SYMFONY_ICU_SELECTOR", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
    @JvmField val OFFSET = createTextAttributesKey("SYMFONY_ICU_OFFSET", DefaultLanguageHighlighterColors.KEYWORD)
    @JvmField val NUMBER = createTextAttributesKey("SYMFONY_ICU_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    @JvmField val HASH = createTextAttributesKey("SYMFONY_ICU_HASH", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL)
    @JvmField val QUOTED_TEXT = createTextAttributesKey("SYMFONY_ICU_QUOTED_TEXT", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)
    @JvmField val BAD_CHARACTER = createTextAttributesKey("SYMFONY_ICU_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
}

// message text has no key, so it keeps the color of the string it is injected into
class IcuMessageSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = IcuMessageLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> = pack(KEYS[tokenType])
}

private val KEYS: Map<IElementType, TextAttributesKey> = mapOf(
    IcuMessageTokenTypes.LBRACE to IcuMessageHighlighterColors.BRACES,
    IcuMessageTokenTypes.RBRACE to IcuMessageHighlighterColors.BRACES,
    IcuMessageTokenTypes.COMMA to IcuMessageHighlighterColors.COMMA,
    IcuMessageTokenTypes.ARGUMENT_NAME to IcuMessageHighlighterColors.ARGUMENT_NAME,
    IcuMessageTokenTypes.ARGUMENT_TYPE to IcuMessageHighlighterColors.ARGUMENT_TYPE,
    IcuMessageTokenTypes.ARGUMENT_STYLE to IcuMessageHighlighterColors.ARGUMENT_STYLE,
    IcuMessageTokenTypes.SELECTOR to IcuMessageHighlighterColors.SELECTOR,
    IcuMessageTokenTypes.OFFSET to IcuMessageHighlighterColors.OFFSET,
    IcuMessageTokenTypes.NUMBER to IcuMessageHighlighterColors.NUMBER,
    IcuMessageTokenTypes.HASH to IcuMessageHighlighterColors.HASH,
    IcuMessageTokenTypes.QUOTED_TEXT to IcuMessageHighlighterColors.QUOTED_TEXT,
    IcuMessageTokenTypes.ESCAPED_QUOTE to IcuMessageHighlighterColors.QUOTED_TEXT,
    IcuMessageTokenTypes.UNCLOSED_QUOTE to IcuMessageHighlighterColors.BAD_CHARACTER,
    TokenType.BAD_CHARACTER to IcuMessageHighlighterColors.BAD_CHARACTER,
)

class IcuMessageSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter = IcuMessageSyntaxHighlighter()
}

class IcuMessageColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName(): String = "Symfony ICU Message"

    override fun getIcon(): Icon = Symfony2Icons.TRANSLATION

    override fun getHighlighter(): SyntaxHighlighter = IcuMessageSyntaxHighlighter()

    override fun getDemoText(): String =
        "{count, plural, offset:1 =0 {Nobody} one {{author} and # other} other {{author} and # others}} replied on {day, date, short}.\n" +
            "{gender, select, female {She} other {They}} pay {price, number, ::currency/EUR}, it''s '{free}' {invalid.name}"

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = arrayOf(
        AttributesDescriptor("Braces", IcuMessageHighlighterColors.BRACES),
        AttributesDescriptor("Comma", IcuMessageHighlighterColors.COMMA),
        AttributesDescriptor("Argument name", IcuMessageHighlighterColors.ARGUMENT_NAME),
        AttributesDescriptor("Argument type", IcuMessageHighlighterColors.ARGUMENT_TYPE),
        AttributesDescriptor("Argument style", IcuMessageHighlighterColors.ARGUMENT_STYLE),
        AttributesDescriptor("Option selector", IcuMessageHighlighterColors.SELECTOR),
        AttributesDescriptor("Offset", IcuMessageHighlighterColors.OFFSET),
        AttributesDescriptor("Number", IcuMessageHighlighterColors.NUMBER),
        AttributesDescriptor("Plural number (#)", IcuMessageHighlighterColors.HASH),
        AttributesDescriptor("Quoted text", IcuMessageHighlighterColors.QUOTED_TEXT),
        AttributesDescriptor("Bad character", IcuMessageHighlighterColors.BAD_CHARACTER),
    )

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
}
