package fr.adrienbrault.idea.symfony2plugin.doctrine.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.config.SymfonyPhpReferenceContributor
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import org.apache.commons.lang3.StringUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class DoctrineRepositoryClassConstantIntention : PsiElementBaseIntentionAction() {
    override fun invoke(project: Project, editor: Editor?, psiElement: PsiElement) {
        val parent = psiElement.parent as? StringLiteralExpression
            ?: return

        try {
            val phpClass = EntityHelper.resolveShortcutName(project, parent.contents)
                ?: throw Exception("Can not resolve model class")
            PhpElementsUtil.replaceElementWithClassConstant(phpClass, parent)
        } catch (e: Exception) {
            IdeHelper.showErrorHintIfAvailable(editor!!, e.message!!)
        }
    }

    override fun isAvailable(project: Project, editor: Editor?, psiElement: PsiElement): Boolean {
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false
        }

        val parent = psiElement.parent as? StringLiteralExpression
            ?: return false

        if (StringUtils.isBlank(parent.contents)) {
            return false
        }

        return MethodMatcher.StringParameterMatcher(parent, 0)
            .withSignature(SymfonyPhpReferenceContributor.REPOSITORY_SIGNATURES)
            .withSignature("Doctrine\\Persistence\\ObjectManager", "find")
            .withSignature("Doctrine\\Common\\Persistence\\ObjectManager", "find") // @TODO: missing somewhere
            .match() != null
    }

    override fun getFamilyName(): String {
        return "Doctrine: use class constant"
    }

    override fun getText(): String {
        return familyName
    }
}
