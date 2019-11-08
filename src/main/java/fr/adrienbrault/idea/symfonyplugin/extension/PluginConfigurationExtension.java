package fr.adrienbrault.idea.symfony2plugin.extension;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface PluginConfigurationExtension {
    void invokePluginConfiguration(@NotNull PluginConfigurationExtensionParameter pluginConfigurationExtensionParameter);
}
