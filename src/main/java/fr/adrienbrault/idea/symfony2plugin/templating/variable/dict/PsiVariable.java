package fr.adrienbrault.idea.symfony2plugin.templating.variable.dict;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PsiVariable {
    @NotNull
    final private Set<String> types = new HashSet<>();

    @NotNull
    final private Collection<PsiElement> psiElements = new HashSet<>();

    public PsiVariable(@NotNull Set<String> types, @Nullable PsiElement psiElement) {
        this.types.addAll(types);
        this.psiElements.add(psiElement);
    }

    public PsiVariable(@NotNull Set<String> types) {
        this.types.addAll(types);
    }

    public PsiVariable(@NotNull String type) {
        this.types.add(type);
    }

    public PsiVariable() {
    }

    @NotNull
    public Set<String> getTypes() {
        return types;
    }

    @Nullable
    public PsiElement getElement() {
        if (psiElements.size() > 0) {
            return psiElements.iterator().next();
        }

        return null;
    }

    public void addElements(@NotNull PsiElement psiElement) {
        this.psiElements.add(psiElement);
    }

    public void addTypes(@NotNull Collection<String> types) {
        this.types.addAll(types);
    }

    public void addType(@NotNull String type) {
        this.types.add(type);
    }
}
