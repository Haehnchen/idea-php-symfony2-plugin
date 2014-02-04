package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface TwigTypeResolver {
    public void resolve(Collection<TwigTypeContainer> targets, @Nullable Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements);
}
