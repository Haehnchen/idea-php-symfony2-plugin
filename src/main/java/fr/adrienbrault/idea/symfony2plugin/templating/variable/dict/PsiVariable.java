package fr.adrienbrault.idea.symfony2plugin.templating.variable.dict;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PsiVariable {
    @NotNull
    final private Set<String> types;

    @NotNull
    final private Set<String> formTypeFqns;

    public PsiVariable(@NotNull Set<String> types, @NotNull Collection<String> formTypeFqns) {
        this.types = Collections.unmodifiableSet(new HashSet<>(types));
        this.formTypeFqns = Collections.unmodifiableSet(new HashSet<>(formTypeFqns));
    }

    public PsiVariable(@NotNull Set<String> types) {
        this(types, Collections.emptySet());
    }

    public PsiVariable(@NotNull String type) {
        this(Collections.singleton(type));
    }

    public PsiVariable() {
        this(Collections.emptySet());
    }

    @NotNull
    public Set<String> getTypes() {
        return types;
    }

    @NotNull
    public Set<String> getFormTypeFqns() {
        return formTypeFqns;
    }
}
