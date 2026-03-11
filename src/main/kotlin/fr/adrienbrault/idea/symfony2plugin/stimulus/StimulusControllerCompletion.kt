package fr.adrienbrault.idea.symfony2plugin.stimulus

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StimulusController
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.StimulusControllerStubIndex

/**
 * Utility class for Stimulus controller completion.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
object StimulusControllerCompletion {

    @JvmStatic
    fun getAllControllers(project: Project): Map<String, StimulusController> {
        val controllers = mutableMapOf<String, StimulusController>()
        val keys = FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, project)

        for (key in keys) {
            FileBasedIndex.getInstance()
                .getValues(StimulusControllerStubIndex.KEY, key, GlobalSearchScope.allScope(project))
                .forEach { controller -> controllers[key] = controller }
        }

        return controllers
    }

    @JvmStatic
    fun createLookupElement(controller: StimulusController, forTwig: Boolean): LookupElement {
        val name = if (forTwig) controller.twigName else controller.normalizedName
        val typeText = if (forTwig && controller.hasOriginalName()) controller.normalizedName else "Stimulus"

        return LookupElementBuilder
            .create(name)
            .withTypeText(typeText)
            .withIcon(Symfony2Icons.SYMFONY)
            .withBoldness(true)
    }
}
