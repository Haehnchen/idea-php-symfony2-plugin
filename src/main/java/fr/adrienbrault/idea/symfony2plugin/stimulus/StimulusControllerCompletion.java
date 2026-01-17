package fr.adrienbrault.idea.symfony2plugin.stimulus;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.StimulusControllerStubIndex;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Utility class for Stimulus controller completion.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class StimulusControllerCompletion {

    /**
     * Get all Stimulus controller names from the index
     */
    @NotNull
    public static Collection<String> getAllControllerNames(@NotNull Project project) {
        return FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, project);
    }

    /**
     * Create a lookup element for a Stimulus controller
     */
    @NotNull
    public static LookupElement createLookupElement(@NotNull String controllerName) {
        return LookupElementBuilder
            .create(controllerName)
            .withTypeText("Stimulus")
            .withIcon(Symfony2Icons.SYMFONY)
            .withBoldness(true);
    }
}
