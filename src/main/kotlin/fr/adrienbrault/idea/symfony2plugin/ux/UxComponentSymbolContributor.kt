package fr.adrienbrault.idea.symfony2plugin.ux

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.navigation.NavigationItemExStateless
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil
import icons.TwigIcons

class UxComponentSymbolContributor : ChooseByNameContributorEx {
    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) {
        val project = scope.project ?: return
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return
        }

        UxUtil.getAllComponentNames(project).forEach {
            processor.process(it.name())
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

        UxUtil.getTwigComponentPhpClasses(project, name).forEach { component ->
            processor.process(
                NavigationItemExStateless.create(
                    component,
                    name,
                    component.icon,
                    "TwigComponent (${component.name})",
                    false,
                ),
            )
        }

        UxUtil.getComponentTemplates(project, name).forEach { component ->
            processor.process(
                NavigationItemExStateless.create(
                    component,
                    name,
                    TwigIcons.TwigFileIcon,
                    "TwigComponent (${component.name})",
                    false,
                ),
            )
        }
    }
}
