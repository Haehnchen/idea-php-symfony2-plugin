package fr.adrienbrault.idea.symfony2plugin.translation.icu.lang

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType

class IcuMessageElementType(debugName: String) : IElementType(debugName, IcuMessageLanguage)

object IcuMessageTokenTypes {
    @JvmField val TEXT = IcuMessageElementType("TEXT")
    @JvmField val QUOTED_TEXT = IcuMessageElementType("QUOTED_TEXT")
    @JvmField val ESCAPED_QUOTE = IcuMessageElementType("ESCAPED_QUOTE")
    @JvmField val HASH = IcuMessageElementType("HASH")
    @JvmField val LBRACE = IcuMessageElementType("LBRACE")
    @JvmField val RBRACE = IcuMessageElementType("RBRACE")
    @JvmField val COMMA = IcuMessageElementType("COMMA")
    @JvmField val ARGUMENT_NAME = IcuMessageElementType("ARGUMENT_NAME")
    @JvmField val ARGUMENT_TYPE = IcuMessageElementType("ARGUMENT_TYPE")
    @JvmField val ARGUMENT_STYLE = IcuMessageElementType("ARGUMENT_STYLE")
    @JvmField val UNCLOSED_QUOTE = IcuMessageElementType("UNCLOSED_QUOTE")
    @JvmField val OFFSET = IcuMessageElementType("OFFSET")
    @JvmField val NUMBER = IcuMessageElementType("NUMBER")
    @JvmField val SELECTOR = IcuMessageElementType("SELECTOR")
}

object IcuMessageElementTypes {
    @JvmField val FILE = IFileElementType("ICU_MESSAGE_FILE", IcuMessageLanguage)
    @JvmField val ARGUMENT = IcuMessageElementType("ARGUMENT")
    @JvmField val OPTION = IcuMessageElementType("OPTION")
    @JvmField val SUB_MESSAGE = IcuMessageElementType("SUB_MESSAGE")
}
