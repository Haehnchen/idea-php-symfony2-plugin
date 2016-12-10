package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface GotoCompletionProviderInterfaceEx extends GotoCompletionProviderInterface {
    /**
     * Extended lookup element implementation
     * allowing resultSet modification
     */
    void getLookupElements(@NotNull GotoCompletionProviderLookupArguments arguments);
}
