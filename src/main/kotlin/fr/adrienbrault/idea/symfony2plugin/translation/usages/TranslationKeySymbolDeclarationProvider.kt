package fr.adrienbrault.idea.symfony2plugin.translation.usages

import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolDeclaration
import com.intellij.model.psi.PsiSymbolDeclarationProvider
import com.intellij.model.psi.PsiSymbolService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

/**
 * Lets Ctrl+Click on PHP and XLIFF translation keys open the usages popup, as YAML keys already do.
 */
class TranslationKeySymbolDeclarationProvider : PsiSymbolDeclarationProvider {
    override fun getDeclarations(element: PsiElement, offsetInElement: Int): Collection<PsiSymbolDeclaration> {
        val range = when (element) {
            is StringLiteralExpression -> element.valueRange
            is XmlTag -> element.value.textRange.shiftLeft(element.textRange.startOffset)
            is XmlAttribute -> element.valueElement?.let { ElementManipulators.getValueTextRange(it).shiftRight(it.startOffsetInParent) }
            else -> null
        } ?: return emptyList()

        if (TranslationKeyUsageUtil.getDeclaration(element)?.element != element) {
            return emptyList()
        }

        return listOf(TranslationKeySymbolDeclaration(element, range))
    }

    private class TranslationKeySymbolDeclaration(private val element: PsiElement, private val range: TextRange) : PsiSymbolDeclaration {
        override fun getDeclaringElement(): PsiElement = element

        override fun getRangeInDeclaringElement(): TextRange = range

        override fun getSymbol(): Symbol = PsiSymbolService.getInstance().asSymbol(element)
    }
}
