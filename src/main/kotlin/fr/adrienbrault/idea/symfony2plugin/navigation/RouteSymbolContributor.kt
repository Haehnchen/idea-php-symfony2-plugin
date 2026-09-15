package fr.adrienbrault.idea.symfony2plugin.navigation

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
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
class RouteSymbolContributor : ChooseByNameContributorEx {
    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) {
        val project = scope.project ?: return
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return
        }

        RouteHelper.getAllRoutes(project).values.forEach { processor.process(it.name) }
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters,
    ) {
        val project = parameters.project
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return
        }

        RouteHelper.getMethods(project, name).forEach {
            processor.process(NavigationItemEx(it, name, Symfony2Icons.ROUTE, "Symfony Route"))
        }
    }
}
