package fr.adrienbrault.idea.symfony2plugin.doctrine

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil
import fr.adrienbrault.idea.symfony2plugin.navigation.NavigationItemExStateless
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class EntityTableSymbolContributor : ChooseByNameContributorEx {
    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) {
        val project = scope.project ?: return
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return
        }

        DoctrineMetadataUtil.getTables(project).forEach { processor.process(it.first) }
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

        DoctrineMetadataUtil.getTables(project)
            .filter { it.first == name }
            .forEach { table ->
                PhpElementsUtil.getClassInterface(project, table.second)?.let {
                    processor.process(
                        NavigationItemExStateless.create(it, table.first, Symfony2Icons.DOCTRINE, "Entity Table", false),
                    )
                }
            }
    }
}
