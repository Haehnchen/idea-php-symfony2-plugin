package fr.adrienbrault.idea.symfony2plugin.routing.usages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.psi.PsiElement

/**
 * Creates the dedicated Find Usages handler for route declarations.
 * This path is used when Find Usages starts on the declaration PSI itself,
 * for example on #[Route(name: 'home')], on a YAML route key, or on an XML route id.
 * Searches started from usage strings like path('home') or $router->generate('home')
 * are routed through RouteUsageTargetProvider instead.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class RouteFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
    /**
     * Activates the custom handler only for supported route declaration PSI.
     */
    override fun canFindUsages(element: PsiElement): Boolean = RouteUsageUtil.getRouteDeclarationTarget(element) != null

    /**
     * Normalizes the caret element to the owning route declaration before creating the handler.
     */
    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler? {
        val routeDeclarationTarget = RouteUsageUtil.getRouteDeclarationTarget(element) ?: return null
        return RouteFindUsagesHandler(routeDeclarationTarget)
    }
}
