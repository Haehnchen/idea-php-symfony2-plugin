package fr.adrienbrault.idea.symfony2plugin.templating.variable.dict;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class PsiVariable {

    final private Set<String> types;
    private PsiElement psiElement;

    public PsiVariable(Set<String> types, @Nullable PsiElement psiElement) {
        this.types = types;
        this.psiElement = psiElement;
    }

    public PsiVariable(Set<String> types) {
        this.types = types;
    }

    public Set<String> getTypes() {
        return types;
    }

    @Nullable
    public PsiElement getElement() {
        return psiElement;
    }

}
