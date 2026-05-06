package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigFileVariableCollectorParameter {

    private final PsiElement psiElement;
    private final Set<VirtualFile> visitedFiles;
    private final Project project;
    private final PsiFile containingFile;

    public TwigFileVariableCollectorParameter(@NotNull PsiElement psiElement, @NotNull PsiFile containingFile, @NotNull Set<VirtualFile> visitedFiles) {
        this.psiElement = psiElement;
        this.project = psiElement.getProject();
        this.containingFile = containingFile;
        this.visitedFiles = visitedFiles;
    }

    public PsiElement getElement() {
        return psiElement;
    }

    public Project getProject() {
        return project;
    }

    @NotNull
    public PsiFile getContainingFile() {
        return containingFile;
    }

    public Set<VirtualFile> getVisitedFiles() {
        return visitedFiles;
    }

}
