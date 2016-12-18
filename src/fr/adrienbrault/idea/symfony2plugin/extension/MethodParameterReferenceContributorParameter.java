package fr.adrienbrault.idea.symfony2plugin.extension;

import com.intellij.openapi.project.Project;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MethodParameterReferenceContributorParameter {

    private final Project project;

    public MethodParameterReferenceContributorParameter(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

}
