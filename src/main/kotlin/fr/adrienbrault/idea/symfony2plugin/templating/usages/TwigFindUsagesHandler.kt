package fr.adrienbrault.idea.symfony2plugin.templating.usages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern

/**
 * Handles Find Usages started from Twig, either by delegating to resolved PHP targets or by
 * searching exact Twig extension symbol names when the Twig symbol itself is the usage identity.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigFindUsagesHandler(
    private val target: TwigFindUsagesTarget,
) : FindUsagesHandler(target.primaryElement) {
    /**
     * Exposes the effective primary search targets for the Usage View.
     */
    override fun getPrimaryElements(): Array<PsiElement> = target.primaryElements.toTypedArray()

    /**
     * Delegates member/class usages to PHP targets and searches Twig extension symbols directly by name.
     */
    override fun processElementUsages(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        options: FindUsagesOptions,
    ): Boolean {
        return when (target) {
            is TwigPhpFindUsagesTarget -> {
                val targets = getEffectivePhpTargets(element, target.phpTargets)

                for (phpTarget in targets) {
                    if (!super.processElementUsages(phpTarget, processor, options)) {
                        return false
                    }
                }

                true
            }
            is TwigExtensionSymbolFindUsagesTarget -> processTwigExtensionUsages(target.symbol, processor, options)
        }
    }

    /**
     * Applies the same Twig-to-PHP delegation for plain text occurrences.
     */
    override fun processUsagesInText(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        searchScope: GlobalSearchScope,
    ): Boolean {
        return when (target) {
            is TwigPhpFindUsagesTarget -> {
                val targets = getEffectivePhpTargets(element, target.phpTargets)

                for (phpTarget in targets) {
                    if (!super.processUsagesInText(phpTarget, processor, searchScope)) {
                        return false
                    }
                }

                true
            }
            is TwigExtensionSymbolFindUsagesTarget -> true
        }
    }

    /**
     * Supports both direct calls with the original Twig PSI and platform calls with one of the primary PHP targets.
     */
    private fun getEffectivePhpTargets(element: PsiElement, phpTargets: List<PsiElement>): List<PsiElement> {
        return phpTargets.firstOrNull { it.isEquivalentTo(element) }?.let { listOf(it) } ?: phpTargets
    }

    /**
     * Searches Twig files directly for the exact extension symbol name under Find Usages, for example `form_start` in `{{ form_start() }}`.
     */
    private fun processTwigExtensionUsages(
        symbol: TwigExtensionUsageSymbol,
        processor: Processor<in UsageInfo>,
        options: FindUsagesOptions,
    ): Boolean {
        return ApplicationManager.getApplication().runReadAction<Boolean> {
            val project = target.primaryElement.project
            val searchScope = options.searchScope as? GlobalSearchScope ?: GlobalSearchScope.projectScope(project)
            val twigScope = GlobalSearchScope.getScopeRestrictedByFileTypes(searchScope, TwigFileType.INSTANCE)

            for (twigFile in TwigMethodReferencesSearchExecutor().collectTwigFiles(project, twigScope, setOf(symbol.name))) {
                twigFile.accept(object : PsiRecursiveElementWalkingVisitor() {
                    override fun visitElement(element: PsiElement) {
                        if (matchesTwigExtensionSymbol(element, symbol)) {
                            processor.process(UsageInfo(element))
                        }

                        super.visitElement(element)
                    }
                })
            }

            true
        }
    }
}

/**
 * Describes the effective identity of a Twig Find Usages session.
 */
sealed interface TwigFindUsagesTarget {
    /**
     * Anchor PSI element used to create the handler and open the Usage View.
     */
    val primaryElement: PsiElement

    /**
     * Effective search targets exposed to the Usage View, either PHP delegates or the Twig symbol itself.
     */
    val primaryElements: List<PsiElement>
}

/**
 * Twig usage target that delegates the search to one or more resolved PHP elements.
 */
data class TwigPhpFindUsagesTarget(
    override val primaryElement: PsiElement,
    val phpTargets: List<PsiElement>,
) : TwigFindUsagesTarget {
    override val primaryElements: List<PsiElement> = phpTargets
}

/**
 * Twig extension usage target that keeps the Twig symbol itself as the usage identity, for example `form_start` or `trans`.
 */
data class TwigExtensionSymbolFindUsagesTarget(
    override val primaryElement: PsiElement,
    val symbol: TwigExtensionUsageSymbol,
) : TwigFindUsagesTarget {
    override val primaryElements: List<PsiElement> = listOf(primaryElement)
}

/**
 * Matches only the exact Twig extension symbol leaf, such as `form_start` in `{{ form_start() }}` or `trans` in `{% apply trans %}`.
 */
private fun matchesTwigExtensionSymbol(
    element: PsiElement,
    symbol: TwigExtensionUsageSymbol,
): Boolean {
    if (!element.text.equals(symbol.name, ignoreCase = true)) {
        return false
    }

    return when (symbol.kind) {
        TwigExtensionUsageKind.FUNCTION -> TwigPattern.getPrintBlockFunctionPattern().accepts(element)
        TwigExtensionUsageKind.FILTER -> TwigPattern.getFilterPattern().accepts(element) || TwigPattern.getApplyFilterPattern().accepts(element)
    }
}
