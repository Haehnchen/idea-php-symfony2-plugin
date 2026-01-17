package fr.adrienbrault.idea.symfony2plugin.stimulus;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StimulusController;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.StimulusControllerStubIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for Stimulus controller completion.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class StimulusControllerCompletion {
    /**
     * Get all Stimulus controllers with their full data
     */
    @NotNull
    public static Map<String, StimulusController> getAllControllers(@NotNull Project project) {
        Map<String, StimulusController> controllers = new HashMap<>();
        Collection<String> keys = FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, project);

        for (String key : keys) {
            FileBasedIndex.getInstance().getValues(StimulusControllerStubIndex.KEY, key, GlobalSearchScope.allScope(project))
                .forEach(controller -> controllers.put(key, controller));
        }

        return controllers;
    }
    /**
     * Create a lookup element for a Stimulus controller with full data
     */
    @NotNull
    public static LookupElement createLookupElement(@NotNull StimulusController controller, boolean forTwig) {
        String name = forTwig ? controller.getTwigName() : controller.getNormalizedName();
        String typeText = forTwig && controller.hasOriginalName()
            ? controller.getNormalizedName()  // Show normalized name as type text for Twig
            : "Stimulus";

        return LookupElementBuilder
            .create(name)
            .withTypeText(typeText)
            .withIcon(Symfony2Icons.SYMFONY)
            .withBoldness(true);
    }
}
