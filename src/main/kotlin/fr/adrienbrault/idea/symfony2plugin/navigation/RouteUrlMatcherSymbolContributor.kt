package fr.adrienbrault.idea.symfony2plugin.navigation

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.ChooseByNameContributorEx2
import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class RouteUrlMatcherSymbolContributor : ChooseByNameContributorEx, ChooseByNameContributorEx2 {
    override fun processNames(processor: Processor<in String>, parameters: FindSymbolParameters) {
        val project = parameters.project
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return
        }

        val name = parameters.localPatternName
        if (RouteHelper.hasRoutesForPathWithPlaceholderMatch(project, name)) {
            processor.process(name)
        }
    }

    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) = Unit

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters,
    ) {
        val project = parameters.project
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return
        }

        val targets = mutableSetOf<PsiElement>()
        RouteHelper.getMethodsForPathWithPlaceholderMatchRoutes(project, name).forEach { (route, target) ->
            if (route.path == null || !targets.add(target)) {
                return@forEach
            }

            processor.process(
                NavigationItemPresentableOverwrite.create(
                    target,
                    route.pathPresentable,
                    Symfony2Icons.ROUTE,
                    "Symfony Route",
                    true,
                    name,
                ),
            )
        }
    }
}
