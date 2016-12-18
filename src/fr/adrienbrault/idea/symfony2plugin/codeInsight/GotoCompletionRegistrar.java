package fr.adrienbrault.idea.symfony2plugin.codeInsight;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface GotoCompletionRegistrar {
    void register(GotoCompletionRegistrarParameter registrar);
}
