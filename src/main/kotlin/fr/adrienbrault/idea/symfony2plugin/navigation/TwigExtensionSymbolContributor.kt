package fr.adrienbrault.idea.symfony2plugin.navigation

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigExtensionSymbolContributor : ChooseByNameContributorEx {
    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) {
        val project = scope.project ?: return
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return
        }

        listOf(TwigExtensionParser.getFilters(project), TwigExtensionParser.getFunctions(project)).forEach { extensions ->
            extensions.keys.forEach(processor::process)
        }
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

        listOf(TwigExtensionParser.getFilters(project), TwigExtensionParser.getFunctions(project)).forEach { extensions ->
            extensions.filterKeys { it == name }.values.forEach { extension ->
                TwigExtensionParser.getExtensionTarget(project, extension)?.let {
                    processor.process(
                        NavigationItemEx(
                            it,
                            name,
                            TwigExtensionParser.getIcon(extension.twigExtensionType),
                            "Twig:${extension.twigExtensionType}",
                        ),
                    )
                }
            }
        }
    }
}
