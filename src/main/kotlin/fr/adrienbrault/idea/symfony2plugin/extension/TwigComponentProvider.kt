package fr.adrienbrault.idea.symfony2plugin.extension

/**
 * Extension point for plugins that already know their Symfony UX Twig Component names and templates.
 */
interface TwigComponentProvider {
    /**
     * Returns already-resolved Twig component definitions for the current project.
     *
     * Providers should return an empty collection when they are not applicable to the project.
     * This method is called from completion, navigation, and usage-search paths, so providers must
     * cache their own discovery results and keep uncached fallback work narrow.
     */
    fun getComponents(parameter: TwigComponentProviderParameter): Collection<TwigComponentDefinition>
}
