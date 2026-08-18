package fr.adrienbrault.idea.symfony2plugin.translation

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.formatter.FormatterUtil
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.jetbrains.php.lang.psi.elements.ParameterListOwner
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TranslationDomainGuessTypoQuickFix
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TwigTranslationDomainInspection
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import org.apache.commons.lang3.StringUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class PhpTranslationDomainInspection : LocalInspectionTool() {
    companion object {
        const val MESSAGE = TwigTranslationDomainInspection.MESSAGE
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is StringLiteralExpression) {
                    inspectTranslationDomain(holder, element)
                }
                super.visitElement(element)
            }
        }
    }

    private fun inspectTranslationDomain(holder: ProblemsHolder, psiElement: StringLiteralExpression) {
        val methodReferenceOrNewExpression: ParameterListOwner = TranslationUtil.getTranslationFunctionContext(psiElement)
            ?: return

        val previousNonWhitespaceSibling: ASTNode? = FormatterUtil.getPreviousNonWhitespaceSibling(psiElement.node)

        if (previousNonWhitespaceSibling != null && previousNonWhitespaceSibling.elementType == PhpTokenTypes.opCOLON) {
            val previousNonWhitespaceSibling1 = FormatterUtil.getPreviousNonWhitespaceSibling(previousNonWhitespaceSibling)
            if (previousNonWhitespaceSibling1 != null && previousNonWhitespaceSibling1.elementType == PhpTokenTypes.IDENTIFIER) {
                val text = previousNonWhitespaceSibling1.text
                val isSupportedAttributeInsideContext = "domain" == text &&
                    TranslationUtil.isTranslationReference(methodReferenceOrNewExpression)

                if (isSupportedAttributeInsideContext) {
                    annotateTranslationDomain(psiElement, holder)
                }
            }

            return
        }

        val domainParameter = TranslationUtil.getDomainParameter(methodReferenceOrNewExpression)

        if (domainParameter >= 0) {
            val currentIndex: ParameterBag? = PsiElementUtils.getCurrentParameterIndex(psiElement)
            if (currentIndex != null && currentIndex.index == domainParameter) {
                annotateTranslationDomain(psiElement, holder)
            }
        }
    }

    private fun annotateTranslationDomain(psiElement: StringLiteralExpression, holder: ProblemsHolder) {
        val contents = psiElement.contents
        if (StringUtils.isBlank(contents) || TranslationUtil.hasDomain(psiElement.project, contents)) {
            return
        }

        holder.registerProblem(psiElement, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, TranslationDomainGuessTypoQuickFix(contents))
    }
}
