package fr.adrienbrault.idea.symfony2plugin.translation.icu.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet

/**
 * Reports what ICU rejects although the message parses: an unknown argument type, a plural or select without "other".
 */
class IcuMessageAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element.node.elementType != IcuMessageTokenTypes.ARGUMENT_TYPE) {
            return
        }

        val type = element.text.lowercase()
        if (type !in IcuMessageSyntax.ARGUMENT_TYPES) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Unknown argument type '${element.text}'").range(element).create()
            return
        }

        if (type !in IcuMessageSyntax.COMPLEX_TYPES) {
            return
        }

        val options = element.parent.node.getChildren(OPTIONS)
        if (options.isNotEmpty() && options.none { it.findChildByType(IcuMessageTokenTypes.SELECTOR)?.text == "other" }) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Missing 'other' option in $type").range(element).create()
        }
    }
}

private val OPTIONS = TokenSet.create(IcuMessageElementTypes.OPTION)
