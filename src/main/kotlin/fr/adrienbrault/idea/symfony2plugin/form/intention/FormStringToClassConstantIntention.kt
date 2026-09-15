package fr.adrienbrault.idea.symfony2plugin.form.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher
import org.apache.commons.lang3.StringUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class FormStringToClassConstantIntention : PsiElementBaseIntentionAction() {
    override fun invoke(project: Project, editor: Editor?, psiElement: PsiElement) {
        val parent = psiElement.parent as? StringLiteralExpression
            ?: return

        try {
            FormUtil.replaceFormStringAliasWithClassConstant(parent)
        } catch (e: Exception) {
            IdeHelper.showErrorHintIfAvailable(editor!!, e.message!!)
        }
    }

    override fun isAvailable(project: Project, editor: Editor?, psiElement: PsiElement): Boolean {
        if (!Symfony2ProjectComponent.isEnabled(psiElement.project)) {
            return false
        }

        val parent = psiElement.parent as? StringLiteralExpression
            ?: return false

        if (StringUtils.isBlank(parent.contents)) {
            return false
        }

        return MethodMatcher.StringParameterMatcher(parent, 1)
            .withSignature(FormUtil.PHP_FORM_BUILDER_SIGNATURES)
            .match() != null
    }

    override fun getFamilyName(): String {
        return "Symfony: use FormType class constant"
    }

    override fun getText(): String {
        return familyName
    }
}
