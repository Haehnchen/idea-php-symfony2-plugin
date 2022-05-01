package fr.adrienbrault.idea.symfony2plugin.webDeployment;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class WebDeploymentUtil {

    private static Boolean PLUGIN_ENABLED;

    public static boolean isEnabled(@Nullable Project project) {
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        if(PLUGIN_ENABLED == null) {
            IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("com.jetbrains.plugins.webDeployment"));
            PLUGIN_ENABLED = plugin != null && plugin.isEnabled();
        }

        return PLUGIN_ENABLED;
    }

}
