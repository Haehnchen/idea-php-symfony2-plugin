package fr.adrienbrault.idea.symfony2plugin.templating.usages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor

/**
 * Delegates Twig-started Find Usages to the resolved PHP targets instead of searching the raw Twig PSI locally.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigTypeFindUsagesHandler(
    twigElement: PsiElement,
    private val phpTargets: List<PsiElement>,
) : FindUsagesHandler(twigElement) {
    /**
     * Exposes the resolved PHP targets as the primary search targets for the Usage View.
     */
    override fun getPrimaryElements(): Array<PsiElement> = phpTargets.toTypedArray()

    /**
     * Delegates the actual search work to the PHP targets so existing PHP and Twig reference search logic is reused.
     */
    override fun processElementUsages(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        options: FindUsagesOptions,
    ): Boolean {
        val targets = getEffectiveTargets(element)

        for (target in targets) {
            if (!super.processElementUsages(target, processor, options)) {
                return false
            }
        }

        return true
    }

    /**
     * Applies the same Twig-to-PHP delegation for plain text occurrences.
     */
    override fun processUsagesInText(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        searchScope: GlobalSearchScope,
    ): Boolean {
        val targets = getEffectiveTargets(element)

        for (target in targets) {
            if (!super.processUsagesInText(target, processor, searchScope)) {
                return false
            }
        }

        return true
    }

    /**
     * Supports both direct calls with the original Twig PSI and platform calls with one of the primary PHP targets.
     */
    private fun getEffectiveTargets(element: PsiElement): List<PsiElement> {
        return phpTargets.firstOrNull { it.isEquivalentTo(element) }?.let { listOf(it) } ?: phpTargets
    }
}
