package fr.adrienbrault.idea.symfony2plugin.intentions.php

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpBundleFileFactory

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class PhpBundleCompilerPassIntention : PsiElementBaseIntentionAction() {
    override fun invoke(project: Project, editor: Editor?, psiElement: PsiElement) {
        if (psiElement.containingFile !is PhpFile) {
            return
        }

        val phpClass = PhpBundleFileFactory.getPhpClassForCreateCompilerScope(PsiTreeUtil.getParentOfType(psiElement, PhpClass::class.java))
            ?: return

        PhpBundleFileFactory.invokeCreateCompilerPass(phpClass, editor)
    }

    override fun isAvailable(project: Project, editor: Editor?, psiElement: PsiElement): Boolean {
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false
        }

        if (psiElement.containingFile !is PhpFile) {
            return false
        }

        return PhpBundleFileFactory.getPhpClassForCreateCompilerScope(PsiTreeUtil.getParentOfType(psiElement, PhpClass::class.java)) != null
    }

    override fun getFamilyName(): String {
        return "Symfony: Create CompilerPass"
    }

    override fun getText(): String {
        return familyName
    }
}
