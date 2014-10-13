package fr.adrienbrault.idea.symfony2plugin.remote.httpHandler;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fr.adrienbrault.idea.symfony2plugin.remote.util.HttpExchangeUtil;
import java.io.IOException;

public class InfoActionHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        StringBuilder builder = new StringBuilder();

        builder.append("FullVersion: ").append(ApplicationInfo.getInstance().getFullVersion()).append("\n");
        builder.append("ApiVersion: ").append(ApplicationInfo.getInstance().getApiVersion()).append("\n");

        IdeaPluginDescriptor descriptor = PluginManager.getPlugin(PluginId.getId("fr.adrienbrault.idea.symfony2plugin"));
        if(descriptor != null) {
            builder.append("Symfony2 Plugin: ").append(descriptor.getVersion()).append("\n");
        }

        HttpExchangeUtil.sendResponse(httpExchange, builder);
    }

}
