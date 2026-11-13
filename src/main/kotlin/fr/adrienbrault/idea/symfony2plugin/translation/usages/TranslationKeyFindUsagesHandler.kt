package fr.adrienbrault.idea.symfony2plugin.translation.usages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import com.intellij.util.xml.model.gotosymbol.GoToSymbolProvider
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import javax.swing.Icon

class TranslationKeyFindUsagesHandler(private val declaration: TranslationKeyDeclaration) : FindUsagesHandler(declaration.element) {
    override fun getPrimaryElements(): Array<PsiElement> = arrayOf(TranslationKeyNavigationItem(declaration))

    override fun processElementUsages(element: PsiElement, processor: Processor<in UsageInfo>, options: FindUsagesOptions): Boolean =
        super.processElementUsages(unwrap(element), processor, options)

    override fun processUsagesInText(element: PsiElement, processor: Processor<in UsageInfo>, searchScope: GlobalSearchScope): Boolean =
        super.processUsagesInText(unwrap(element), processor, searchScope)

    private fun unwrap(element: PsiElement): PsiElement = (element as? TranslationKeyNavigationItem)?.declaration?.element ?: element

    class TranslationKeyNavigationItem(val declaration: TranslationKeyDeclaration) : GoToSymbolProvider.BaseNavigationItem(declaration.element, "", null) {
        override fun getIcon(flags: Boolean): Icon = Symfony2Icons.TRANSLATION

        override fun getPresentation(): ItemPresentation = object : ItemPresentation {
            override fun getPresentableText(): String = declaration.key

            override fun getLocationString(): String = TranslationKeyUsageUtil.getLocationString(declaration)

            override fun getIcon(open: Boolean): Icon = Symfony2Icons.TRANSLATION
        }
    }
}
