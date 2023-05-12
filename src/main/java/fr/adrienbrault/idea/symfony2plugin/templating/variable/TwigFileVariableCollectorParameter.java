package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigFileVariableCollectorParameter {

    private final PsiElement psiElement;
    private final Set<VirtualFile> visitedFiles;
    private final Project project
        ;

    public TwigFileVariableCollectorParameter(@NotNull PsiElement psiElement, @NotNull Set<VirtualFile> visitedFiles) {
        this.psiElement = psiElement;
        this.project = psiElement.getProject();
        this.visitedFiles = visitedFiles;
    }

    public PsiElement getElement() {
        return psiElement;
    }

    public Project getProject() {
        return psiElement.getProject();
    }

    public Set<VirtualFile> getVisitedFiles() {
        return visitedFiles;
    }

}
