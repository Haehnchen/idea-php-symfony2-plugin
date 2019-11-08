package fr.adrienbrault.idea.symfonyplugin.extension;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigNamespaceExtensionParameter {

    @NotNull
    private final Project project;

    public TwigNamespaceExtensionParameter(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public Project getProject() {
        return project;
    }
}
