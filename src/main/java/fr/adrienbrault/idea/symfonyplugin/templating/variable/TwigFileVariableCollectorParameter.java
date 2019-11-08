package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigFileVariableCollectorParameter {

    private final PsiElement psiElement;
    private final Set<VirtualFile> visitedFiles;

    public TwigFileVariableCollectorParameter(PsiElement psiElement, Set<VirtualFile> visitedFiles) {
        this.psiElement = psiElement;
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
