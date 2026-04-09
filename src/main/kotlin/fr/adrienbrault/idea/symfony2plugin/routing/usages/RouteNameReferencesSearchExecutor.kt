package fr.adrienbrault.idea.symfony2plugin.routing.usages

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.twig.TwigFile
import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.routing.RouteReference
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigRouteUsageStubIndex

/**
 * Collects route usages for Find Usages.
 * Twig usages come from the stub index, PHP usages are found by literal word search and filtered down to real route references.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class RouteNameReferencesSearchExecutor : QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {
    /**
     * Entry point used by IntelliJ's reference search infrastructure for route declarations.
     */
    override fun execute(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ): Boolean {
        ApplicationManager.getApplication().runReadAction {
            val target = queryParameters.elementToSearch
            val routeName = RouteUsageUtil.getRouteNameForDeclaration(target) ?: return@runReadAction
            val declarationTarget = RouteUsageUtil.getRouteDeclarationTarget(target) ?: return@runReadAction
            doSearch(target.project, routeName, declarationTarget, queryParameters, consumer)
        }

        return true
    }

    /**
     * Runs the actual search over Twig and PHP files and emits synthetic references to the declaration target.
     */
    private fun doSearch(
        project: Project,
        routeName: String,
        declarationTarget: PsiElement,
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ) {
        val index = FileBasedIndex.getInstance()
        val searchScope: SearchScope = queryParameters.effectiveSearchScope
        val scope = searchScope as? GlobalSearchScope ?: GlobalSearchScope.projectScope(project)
        val twigScope = GlobalSearchScope.getScopeRestrictedByFileTypes(scope, TwigFileType.INSTANCE)
        val psiManager = PsiManager.getInstance(project)
        val processed = HashSet<PsiElement>()

        index.processValues(TwigRouteUsageStubIndex.KEY, routeName, null, { file: VirtualFile, _: Set<String> ->
            val psiFile: PsiFile = psiManager.findFile(file) ?: return@processValues true
            if (psiFile !is TwigFile) {
                return@processValues true
            }

            for (usage in TwigRouteUsageStubIndex.getRouteUsages(psiFile, setOf(routeName))) {
                if (!processed.add(usage.target)) {
                    continue
                }

                consumer.process(RouteUsageReference(usage.target, declarationTarget, TextRange(0, usage.target.textLength)))
            }

            true
        }, twigScope)

        PsiSearchHelper.getInstance(project).processAllFilesWithWordInLiterals(routeName, scope) { psiFile ->
            if (psiFile !is PhpFile) {
                return@processAllFilesWithWordInLiterals true
            }

            for (literalExpression in PsiTreeUtil.findChildrenOfType(psiFile, StringLiteralExpression::class.java)) {
                if (routeName != literalExpression.contents || !processed.add(literalExpression)) {
                    continue
                }

                if (!isPhpRouteUsage(literalExpression)) {
                    continue
                }

                consumer.process(
                    RouteUsageReference(
                        literalExpression,
                        declarationTarget,
                        TextRange(1, 1 + routeName.length),
                    )
                )
            }

            true
        }
    }

    /**
     * Guards the PHP literal scan so only literals that already resolve as route references are counted as usages.
     */
    private fun isPhpRouteUsage(literalExpression: StringLiteralExpression): Boolean {
        for (reference in literalExpression.references) {
            if (reference is RouteReference) {
                return true
            }
        }

        return false
    }

    /**
     * Synthetic reference used for usages produced by the custom route search pipeline instead of native PSI references.
     */
    class RouteUsageReference(
        sourceElement: PsiElement,
        private val targetElement: PsiElement,
        rangeInElement: TextRange,
    ) : PsiReferenceBase<PsiElement>(sourceElement, rangeInElement, false) {
        /**
         * Resolves the synthetic usage back to the route declaration PSI.
         */
        override fun resolve(): PsiElement = targetElement

        /**
         * Find Usages does not need completion variants for these synthetic references.
         */
        override fun getVariants(): Array<Any> = emptyArray()
    }
}
