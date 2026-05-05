package fr.adrienbrault.idea.symfony2plugin.templating.variable.dict;

import org.jetbrains.annotations.NotNull;

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
    final private Set<String> formTypeFqns = new HashSet<>();

    public PsiVariable(@NotNull Set<String> types, @NotNull Collection<String> formTypeFqns) {
        this.types.addAll(types);
        this.formTypeFqns.addAll(formTypeFqns);
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

    @NotNull
    public Set<String> getFormTypeFqns() {
        return formTypeFqns;
    }

    public void addTypes(@NotNull Collection<String> types) {
        this.types.addAll(types);
    }

    public void addType(@NotNull String type) {
        this.types.add(type);
    }

    public void addFormTypeFqns(@NotNull Collection<String> formTypeFqns) {
        this.formTypeFqns.addAll(formTypeFqns);
    }
}
