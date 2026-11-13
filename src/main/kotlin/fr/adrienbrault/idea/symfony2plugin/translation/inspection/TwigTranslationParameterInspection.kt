package fr.adrienbrault.idea.symfony2plugin.translation.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.twig.TwigTokenTypes
import com.jetbrains.twig.elements.TwigElementTypes
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigFunctionHashParser
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils.isInterpolatedString
import org.apache.commons.lang3.StringUtils

/**
 * {{ 'key'|trans({unknown: 1}) }}
 */
class TwigTranslationParameterInspection : LocalInspectionTool() {
    companion object {
        const val MESSAGE = "Symfony: Unknown translation parameter"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.node?.elementType == TwigElementTypes.FUNCTION_CALL) {
                    inspectFilter(holder, element)
                }

                super.visitElement(element)
            }
        }
    }

    private fun inspectFilter(holder: ProblemsHolder, functionCall: PsiElement) {
        val filter = PsiElementUtils.getChildrenOfType(functionCall, PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER))?.text
        val (argumentsIndex, domainIndex) = when (filter) {
            "trans" -> 0 to 1
            "transchoice", "trans_html" -> 1 to 2
            else -> return
        }

        val keyElement = getTranslationKey(functionCall) ?: return
        val key = keyElement.text
        if (StringUtils.isBlank(key) || isInterpolatedString(key)) {
            return
        }

        val arguments = TwigFunctionHashParser.getArguments(functionCall)
        val parameters = findArgument(arguments, "arguments", argumentsIndex)?.value ?: return
        if (!TwigFunctionHashParser.isHashLiteral(parameters)) {
            return
        }

        val domain = findArgument(arguments, "domain", domainIndex)?.value
        if (domain != null && !isQuote(domain)) {
            return
        }

        TranslationParameterInspectionUtil.registerUnknownParameters(
            holder,
            key,
            TwigUtil.getPsiElementTranslationDomain(keyElement),
            TwigFunctionHashParser.getHashKeys(parameters),
        )
    }

    // the string right before "|trans(", e.g. not "a" in {{ 'a'|trans ~ name|trans({}) }}
    private fun getTranslationKey(functionCall: PsiElement): PsiElement? {
        val filter = PsiTreeUtil.prevVisibleLeaf(functionCall)?.takeIf { it.node.elementType == TwigTokenTypes.FILTER } ?: return null
        val quote = PsiTreeUtil.prevVisibleLeaf(filter)?.takeIf(::isQuote) ?: return null
        return PsiTreeUtil.prevVisibleLeaf(quote)?.takeIf { it.node.elementType == TwigTokenTypes.STRING_TEXT }
    }

    private fun isQuote(element: PsiElement): Boolean =
        element.node.elementType == TwigTokenTypes.SINGLE_QUOTE || element.node.elementType == TwigTokenTypes.DOUBLE_QUOTE

    private fun findArgument(arguments: List<TwigFunctionHashParser.FunctionArgument>, name: String, index: Int): TwigFunctionHashParser.FunctionArgument? =
        arguments.find { if (it.name != null) it.name == name else it.index == index }
}
