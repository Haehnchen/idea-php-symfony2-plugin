package fr.adrienbrault.idea.symfony2plugin.translation.usages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.psi.PsiElement

class TranslationKeyFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
    override fun canFindUsages(element: PsiElement): Boolean = TranslationKeyUsageUtil.getDeclaration(element) != null

    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler? {
        val declaration = TranslationKeyUsageUtil.getDeclaration(element) ?: return null
        return TranslationKeyFindUsagesHandler(declaration)
    }
}
