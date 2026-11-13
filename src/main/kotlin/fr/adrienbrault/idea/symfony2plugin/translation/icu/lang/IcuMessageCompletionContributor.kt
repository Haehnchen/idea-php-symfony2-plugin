package fr.adrienbrault.idea.symfony2plugin.translation.icu.lang

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.ProcessingContext

/**
 * {sum, <caret>}, {count, plural, <caret>}, {price, number, <caret>}
 */
class IcuMessageCompletionContributor : CompletionContributor() {
    init {
        extend(IcuMessageTokenTypes.ARGUMENT_TYPE) { IcuMessageSyntax.ARGUMENT_TYPES }
        extend(IcuMessageTokenTypes.SELECTOR) { position ->
            when (val type = getArgumentType(position)) {
                in IcuMessageSyntax.PLURAL_TYPES -> (if (type == "plural") listOf("offset:") else emptyList()) + IcuMessageSyntax.PLURAL_SELECTORS
                "select" -> listOf("other")
                else -> emptyList()
            }
        }
        extend(IcuMessageTokenTypes.ARGUMENT_STYLE) { position ->
            when (getArgumentType(position)) {
                "number" -> IcuMessageSyntax.NUMBER_STYLES
                "date", "time" -> IcuMessageSyntax.DATE_STYLES
                else -> emptyList()
            }
        }
    }

    private fun extend(tokenType: IElementType, keywords: (PsiElement) -> List<String>) {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(tokenType), object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                keywords(parameters.position).forEach { result.addElement(LookupElementBuilder.create(it).bold()) }
            }
        })
    }

    private fun getArgumentType(position: PsiElement): String? =
        generateSequence(position.parent) { it.parent }
            .firstOrNull { it.node?.elementType == IcuMessageElementTypes.ARGUMENT }
            ?.node?.findChildByType(IcuMessageTokenTypes.ARGUMENT_TYPE)?.text?.lowercase()
}
