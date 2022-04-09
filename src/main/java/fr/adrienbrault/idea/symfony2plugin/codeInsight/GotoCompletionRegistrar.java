package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @deprecated Use core features
 */
public interface GotoCompletionRegistrar {
    void register(@NotNull GotoCompletionRegistrarParameter registrar);
}
