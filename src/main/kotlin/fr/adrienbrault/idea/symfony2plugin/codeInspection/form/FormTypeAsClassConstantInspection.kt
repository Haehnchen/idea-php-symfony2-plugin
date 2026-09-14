package fr.adrienbrault.idea.symfony2plugin.codeInspection.form

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.util.ArrayUtil
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyUtil

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class FormTypeAsClassConstantInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private var isVersionGreaterThenEquals: Boolean? = null

        override fun visitElement(element: PsiElement) {
            if (element !is MethodReference ||
                !ArrayUtil.contains(element.name, "add", "create") ||
                !PhpElementsUtil.isMethodReferenceInstanceOf(element, "Symfony\\Component\\Form\\FormBuilderInterface")
            ) {
                super.visitElement(element)
                return
            }

            if (isVersionGreaterThenEquals == null) {
                isVersionGreaterThenEquals = SymfonyUtil.isVersionGreaterThenEquals(holder.project, "2.8")
            }

            if (isVersionGreaterThenEquals == false) {
                return
            }

            val parameters = element.parameters
            if (parameters.size < 2) {
                super.visitElement(element)
                return
            }

            val formType = parameters[1]
            if (formType !is StringLiteralExpression || formType.contents.contains("\\")) {
                super.visitElement(element)
                return
            }

            holder.registerProblem(
                formType,
                MESSAGE,
                ProblemHighlightType.WEAK_WARNING,
                MyLocalQuickFix(formType)
            )

            super.visitElement(element)
        }

        private class MyLocalQuickFix(element: PsiElement) : LocalQuickFixOnPsiElement(element) {
            override fun getText(): String {
                return "Use class constant"
            }

            override fun getFamilyName(): String {
                return "Class constant"
            }

            override fun invoke(project: Project, psiFile: PsiFile, psiElement: PsiElement, psiElement1: PsiElement) {
                val startElement = startElement
                if (startElement !is StringLiteralExpression) {
                    return
                }

                try {
                    FormUtil.replaceFormStringAliasWithClassConstant(startElement)
                } catch (_: Exception) {
                }
            }
        }
    }

    companion object {
        const val MESSAGE = "Use fully-qualified class name (FQCN)"
    }
}
