package fr.adrienbrault.idea.symfony2plugin.extension;

import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface TwigNamespaceExtension {
    @NotNull
    Collection<TwigPath> getNamespaces(@NotNull TwigNamespaceExtensionParameter parameter);
}
