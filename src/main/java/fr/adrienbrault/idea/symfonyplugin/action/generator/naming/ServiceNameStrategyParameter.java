package fr.adrienbrault.idea.symfony2plugin.action.generator.naming;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceNameStrategyParameter {

    @NotNull
    private final Project project;

    @NotNull
    private final String className;

    public ServiceNameStrategyParameter(@NotNull Project project, @NotNull String className) {
        this.project = project;
        this.className = className;
    }

    @NotNull
    public String getClassName() {
        return className;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

}
