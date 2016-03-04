package fr.adrienbrault.idea.symfony2plugin.webDeployment.routing;

import fr.adrienbrault.idea.symfony2plugin.extension.RoutingLoader;
import fr.adrienbrault.idea.symfony2plugin.extension.RoutingLoaderParameter;
import fr.adrienbrault.idea.symfony2plugin.routing.webDeployment.RoutingRemoteFileStorage;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.utils.RemoteWebServerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class WebDeploymentRoutingLoader implements RoutingLoader {
    @Override
    public void invoke(@NotNull RoutingLoaderParameter parameter) {

        // add remote first;
        RoutingRemoteFileStorage extensionInstance = RemoteWebServerUtil
            .getExtensionInstance(parameter.getProject(), RoutingRemoteFileStorage.class);

        if(extensionInstance == null) {
            return;
        }

        parameter.addRoutes(extensionInstance.getState().values());
    }
}
