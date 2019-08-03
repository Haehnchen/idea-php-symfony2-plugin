package fr.adrienbrault.idea.symfony2plugin.extension;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PluginConfigurationExtensionParameter {
    @NotNull
    private final Project project;

    private final Set<String> templateUsageMethod = new HashSet<>();

    public PluginConfigurationExtensionParameter(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    public void addTemplateUsageMethod(@NotNull String methodName) {
        templateUsageMethod.add(methodName);
    }

    public Set<String> getTemplateUsageMethod() {
        return templateUsageMethod;
    }
}
