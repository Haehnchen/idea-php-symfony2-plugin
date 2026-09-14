package fr.adrienbrault.idea.symfony2plugin.templating

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
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
import org.apache.commons.lang3.StringUtils

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
                    invoke(holder, element)
                }
                super.visitElement(element)
            }
        }
    }

    private fun invoke(holder: ProblemsHolder, psiElement: StringLiteralExpression) {
        val templateNameIfMissing = getTemplateNameIfMissing(psiElement) ?: return

        val templateCreateByNameLocalQuickFix: Array<LocalQuickFix> = arrayOf(
            TemplateCreateByNameLocalQuickFix(templateNameIfMissing),
            TemplateGuessTypoQuickFix(templateNameIfMissing)
        )

        holder.registerProblem(
            psiElement,
            "Twig: Missing Template",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            *templateCreateByNameLocalQuickFix
        )
    }

    private fun getTemplateNameIfMissing(psiElement: StringLiteralExpression): String? {
        val parameterBag = PsiElementUtils.getCurrentParameterIndex(psiElement)
        if (parameterBag == null || parameterBag.index != 0) {
            return null
        }

        val parameterList = psiElement.parent
        if (parameterList !is ParameterList) {
            return null
        }

        val methodReference = parameterList.parent
        if (methodReference !is MethodReference) {
            return null
        }

        if (!PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, *SymfonyPhpReferenceContributor.TEMPLATE_SIGNATURES)) {
            return null
        }

        val templateName = PhpElementsUtil.getFirstArgumentStringValue(methodReference)
        if (templateName == null || StringUtils.isBlank(templateName)) {
            return null
        }

        if (TwigUtil.getTemplateFiles(psiElement.project, templateName).isNotEmpty()) {
            return null
        }

        return templateName
    }
}
