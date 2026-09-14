package fr.adrienbrault.idea.symfony2plugin.templating.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.twig.TwigTokenTypes
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import org.apache.commons.lang3.StringUtils

/**
 * {% include 'f<caret>.html.twig' %}
 * {{ include('f<caret>.html.twig') }}
 * {% embed 'f<caret>.html.twig' %}
 * ... and so on
 *
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class TwigTemplateMissingInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element !is LeafPsiElement || element.node.elementType !== TwigTokenTypes.STRING_TEXT) {
                super.visitElement(element)
                return
            }

            if (TwigUtil.isStaticTemplateUsage(element)) {
                inspectTemplate(element, holder)
            }

            super.visitElement(element)
        }
    }
}

private fun inspectTemplate(element: PsiElement, holder: ProblemsHolder) {
    val templateName = element.text
    if (StringUtils.isBlank(templateName)) {
        return
    }

    val psiElements = TwigUtil.getTemplateFiles(element.project, templateName)
    if (psiElements.isNotEmpty()) {
        return
    }

    val templateCreateByNameLocalQuickFix: Array<LocalQuickFix> = arrayOf(
        TemplateCreateByNameLocalQuickFix(templateName),
        TemplateGuessTypoQuickFix(templateName)
    )

    holder.registerProblem(
        element,
        "Twig: Missing Template",
        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
        *templateCreateByNameLocalQuickFix
    )
}
