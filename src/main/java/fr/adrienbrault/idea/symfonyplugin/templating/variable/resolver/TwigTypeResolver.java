package fr.adrienbrault.idea.symfonyplugin.templating.variable.resolver;

import fr.adrienbrault.idea.symfonyplugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfonyplugin.templating.variable.dict.PsiVariable;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface TwigTypeResolver {
    void resolve(Collection<TwigTypeContainer> targets, @Nullable Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable Collection<PsiVariable> psiVariables);
}
