package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;

public abstract class GotoCompletionProvider implements GotoCompletionProviderInterface {

    private final PsiElement element;

    public GotoCompletionProvider(PsiElement element) {
        this.element = element;
    }

    protected Project getProject() {
        return this.element.getProject();
    }

    protected PsiElement getElement() {
        return this.element;
    }

}
