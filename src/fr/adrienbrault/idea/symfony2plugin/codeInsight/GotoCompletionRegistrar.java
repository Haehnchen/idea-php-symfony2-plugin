package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface GotoCompletionRegistrar {
    void register(@NotNull GotoCompletionRegistrarParameter registrar);
}
