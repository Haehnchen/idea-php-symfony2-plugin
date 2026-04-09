package fr.adrienbrault.idea.symfony2plugin.routing.usages

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

/**
 * Custom Find Usages handler for route declarations.
 * It keeps IntelliJ's search behavior but replaces the top-level target node with a presentation wrapper.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class RouteFindUsagesHandler(psiElement: PsiElement) : FindUsagesHandler(psiElement) {
    /**
     * Replaces the raw declaration PSI with a navigation item that can expose icon and gray location text.
     */
    override fun getPrimaryElements(): Array<PsiElement> = arrayOf(RouteFindUsagesNavigationItem(psiElement))

    /**
     * Unwraps the presentation wrapper back to the real declaration before searching usages.
     */
    override fun processElementUsages(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        options: FindUsagesOptions,
    ): Boolean = super.processElementUsages(unwrap(element), processor, options)

    /**
     * Applies the same unwrap logic for text usage searching.
     */
    override fun processUsagesInText(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        searchScope: GlobalSearchScope,
    ): Boolean = super.processUsagesInText(unwrap(element), processor, searchScope)

    /**
     * Presentation-only wrapper for the top Find Usages node of a route declaration.
     * The delegate remains the real PSI target used by the actual search.
     */
    class RouteFindUsagesNavigationItem(val delegate: PsiElement) : GoToSymbolProvider.BaseNavigationItem(delegate, "", null) {
        override fun getIcon(flags: Boolean): Icon = Symfony2Icons.ROUTE

        /**
         * Supplies the UI-facing presentation used by the Usage View header row.
         */
        override fun getPresentation(): ItemPresentation = object : ItemPresentation {
            override fun getPresentableText(): String = RouteUsageUtil.getPresentableText(delegate)

            override fun getLocationString(): String? = RouteUsageUtil.getLocationString(delegate)

            override fun getIcon(open: Boolean): Icon = this@RouteFindUsagesNavigationItem.getIcon(open)
        }
    }

    companion object {
        /**
         * Converts the synthetic navigation item back into the underlying declaration PSI.
         */
        private fun unwrap(element: PsiElement): PsiElement {
            return if (element is RouteFindUsagesNavigationItem) {
                element.delegate
            } else {
                element
            }
        }
    }
}
