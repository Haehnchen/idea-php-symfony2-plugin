package fr.adrienbrault.idea.symfonyplugin.extension;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MethodSignatureTypeProviderParameter {

    private PsiElement psiElement;

    public MethodSignatureTypeProviderParameter(@NotNull PsiElement psiElement) {
        this.psiElement = psiElement;
    }

    public PsiElement getElement() {
        return psiElement;
    }

    public Project getProject() {
        return psiElement.getProject();
    }

}
