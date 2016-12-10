package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public abstract class GotoCompletionProvider implements GotoCompletionProviderInterfaceEx {
    @NotNull
    private final PsiElement element;

    public GotoCompletionProvider(@NotNull PsiElement element) {
        this.element = element;
    }

    @NotNull
    protected Project getProject() {
        return this.element.getProject();
    }

    @NotNull
    protected PsiElement getElement() {
        return this.element;
    }

    @Override
    public void getLookupElements(@NotNull GotoCompletionProviderLookupArguments arguments) {
        // empty for compatibility
    }
}
