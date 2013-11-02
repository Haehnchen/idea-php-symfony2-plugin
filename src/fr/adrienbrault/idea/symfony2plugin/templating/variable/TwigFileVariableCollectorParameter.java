package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;

public class TwigFileVariableCollectorParameter {

    private PsiElement psiElement;

    public TwigFileVariableCollectorParameter(PsiElement psiElement) {
        this.psiElement = psiElement;
    }

    public PsiElement getElement() {
        return psiElement;
    }

    public Project getProject() {
        return psiElement.getProject();
    }

}
