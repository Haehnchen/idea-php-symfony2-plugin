package fr.adrienbrault.idea.symfony2plugin.templating.variable.dict;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PsiVariable {
    @NotNull
    final private Set<String> types;

    @Nullable
    private PsiElement psiElement;

    public PsiVariable(@NotNull Set<String> types, @Nullable PsiElement psiElement) {
        this.types = types;
        this.psiElement = psiElement;
    }

    public PsiVariable(@NotNull Set<String> types) {
        this.types = types;
    }

    public PsiVariable(@NotNull String type) {
        this.types = Collections.singleton(type);;
    }

    @NotNull
    public Set<String> getTypes() {
        return types;
    }

    @Nullable
    public PsiElement getElement() {
        return psiElement;
    }
}
