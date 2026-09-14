package fr.adrienbrault.idea.symfony2plugin.templating

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.config.SymfonyPhpReferenceContributor
import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TemplateCreateByNameLocalQuickFix
import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TemplateGuessTypoQuickFix
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class PhpTemplateMissingInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is StringLiteralExpression) {
                    inspectTemplate(holder, element)
                }
                super.visitElement(element)
            }
        }
    }

    private fun inspectTemplate(holder: ProblemsHolder, psiElement: StringLiteralExpression) {
        val templateName = getTemplateNameIfMissing(psiElement) ?: return

        holder.registerProblem(
            psiElement,
            "Twig: Missing Template",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            TemplateCreateByNameLocalQuickFix(templateName),
            TemplateGuessTypoQuickFix(templateName)
        )
    }

    private fun getTemplateNameIfMissing(psiElement: StringLiteralExpression): String? {
        if (PsiElementUtils.getCurrentParameterIndex(psiElement)?.index != 0) {
            return null
        }

        val parameterList = psiElement.parent as? ParameterList ?: return null
        val methodReference = parameterList.parent as? MethodReference ?: return null

        if (!PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, *SymfonyPhpReferenceContributor.TEMPLATE_SIGNATURES)) {
            return null
        }

        val templateName = PhpElementsUtil.getFirstArgumentStringValue(methodReference)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        if (TwigUtil.getTemplateFiles(psiElement.project, templateName).isNotEmpty()) {
            return null
        }

        return templateName
    }
}
