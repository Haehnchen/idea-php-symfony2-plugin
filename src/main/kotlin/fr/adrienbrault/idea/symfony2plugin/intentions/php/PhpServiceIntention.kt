package fr.adrienbrault.idea.symfony2plugin.intentions.php

import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.action.generator.ServiceGenerateAction

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class PhpServiceIntention : PsiElementBaseIntentionAction() {
    override fun getFileModifierForPreview(target: PsiFile): FileModifier? {
        return null
    }

    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        return IntentionPreviewInfo.EMPTY
    }

    override fun invoke(project: Project, editor: Editor?, psiElement: PsiElement) {
        val parentByCondition = PhpPsiUtil.getParentByCondition<Method>(psiElement, true, Method.INSTANCEOF, null)
        if (parentByCondition == null) {
            return
        }

        val phpClass = PhpPsiUtil.getParentByCondition<PhpClass>(psiElement, true, PhpClass.INSTANCEOF, null)
            ?: return

        ServiceGenerateAction.invokeServiceGenerator(project, phpClass.containingFile, phpClass, editor)
    }

    override fun isAvailable(project: Project, editor: Editor?, psiElement: PsiElement): Boolean {
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false
        }

        val parentByCondition = PhpPsiUtil.getParentByCondition<Method>(psiElement, true, Method.INSTANCEOF, null)
        if (parentByCondition == null) {
            return false
        }

        return PhpPsiUtil.getParentByCondition<PhpClass>(psiElement, true, PhpClass.INSTANCEOF, null) != null
    }

    override fun getFamilyName(): String {
        return "Generate Symfony service"
    }

    override fun getText(): String {
        return familyName
    }
}
