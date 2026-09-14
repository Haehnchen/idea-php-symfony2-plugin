package fr.adrienbrault.idea.symfony2plugin.form.action.generator

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import com.jetbrains.php.lang.psi.PhpCodeEditUtil
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import org.apache.commons.lang3.StringUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class FormTypeConstantMigrationAction : CodeInsightAction() {
    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile) =
        file is PhpFile &&
            Symfony2ProjectComponent.isEnabled(project) &&
            PhpCodeEditUtil.findClassAtCaret(editor, file)?.let {
                PhpElementsUtil.isInstanceOf(it, "Symfony\\Component\\Form\\FormTypeInterface")
            } == true

    override fun getHandler(): CodeInsightActionHandler = MyCodeInsightActionHandler()

    private class MyCodeInsightActionHandler : CodeInsightActionHandler {
        override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
            val phpClass = PhpCodeEditUtil.findClassAtCaret(editor, psiFile)
            if (phpClass == null) {
                IdeHelper.showErrorHintIfAvailable(editor, "No class context found")
                return
            }

            val formTypes = ArrayList<StringLiteralExpression>()
            phpClass.acceptChildren(FormTypeStringElementVisitor(formTypes))

            if (formTypes.isEmpty()) {
                IdeHelper.showErrorHintIfAvailable(editor, "Nothing to do for me")
                return
            }

            for (formType in formTypes) {
                try {
                    FormUtil.replaceFormStringAliasWithClassConstant(formType)
                } catch (_: Exception) {
                }
            }
        }

        private class FormTypeStringElementVisitor(
            private val formTypes: MutableCollection<StringLiteralExpression>
        ) : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is StringLiteralExpression &&
                    !StringUtils.isBlank(element.contents) &&
                    MethodMatcher.StringParameterMatcher(element, 1)
                        .withSignature(FormUtil.PHP_FORM_BUILDER_SIGNATURES)
                        .match() != null
                ) {
                    formTypes.add(element)
                }

                super.visitElement(element)
            }
        }
    }
}
