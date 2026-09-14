package fr.adrienbrault.idea.symfony2plugin.codeInspection.form

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
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
        private val isVersionGreaterThenEquals by lazy(LazyThreadSafetyMode.NONE) {
            SymfonyUtil.isVersionGreaterThenEquals(holder.project, "2.8")
        }

        override fun visitElement(element: PsiElement) {
            if (element !is MethodReference ||
                element.name != "add" && element.name != "create" ||
                !PhpElementsUtil.isMethodReferenceInstanceOf(element, "Symfony\\Component\\Form\\FormBuilderInterface")
            ) {
                super.visitElement(element)
                return
            }

            if (!isVersionGreaterThenEquals) {
                return
            }

            val parameters = element.parameters
            if (parameters.size < 2) {
                super.visitElement(element)
                return
            }

            val formType = parameters[1]
            if (formType !is StringLiteralExpression || "\\" in formType.contents) {
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
            override fun getText() = "Use class constant"

            override fun getFamilyName() = "Class constant"

            override fun invoke(project: Project, psiFile: PsiFile, psiElement: PsiElement, psiElement1: PsiElement) {
                val formType = startElement as? StringLiteralExpression ?: return

                try {
                    FormUtil.replaceFormStringAliasWithClassConstant(formType)
                } catch (_: Exception) {
                }
            }
        }
    }

    companion object {
        const val MESSAGE = "Use fully-qualified class name (FQCN)"
    }
}
