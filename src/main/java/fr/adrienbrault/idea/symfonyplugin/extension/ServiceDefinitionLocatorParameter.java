package fr.adrienbrault.idea.symfony2plugin.extension;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceDefinitionLocatorParameter {

    @NotNull
    private final Project project;

    @NotNull
    private final Collection<PsiElement> psiElements;

    public ServiceDefinitionLocatorParameter(@NotNull Project project, @NotNull Collection<PsiElement> psiElements) {
        this.project = project;
        this.psiElements = psiElements;
    }

    public void addTarget(@NotNull PsiElement psiElement) {
        this.psiElements.add(psiElement);
    }

    public void addTargets(@NotNull Collection<PsiElement> psiElements) {
        this.psiElements.addAll(psiElements);
    }

    @NotNull
    public Project getProject() {
        return project;
    }
}
