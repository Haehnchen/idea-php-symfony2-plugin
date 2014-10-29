package fr.adrienbrault.idea.symfony2plugin.remote.httpHandler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.io.StreamUtil;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fr.adrienbrault.idea.symfony2plugin.remote.RemoteStorage;
import fr.adrienbrault.idea.symfony2plugin.remote.provider.ProviderInterface;
import fr.adrienbrault.idea.symfony2plugin.remote.util.HttpExchangeUtil;
import fr.adrienbrault.idea.symfony2plugin.remote.util.RemoteUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class ProjectPostActionHandler {

    @Nullable
    private Project getProject(String name) {

        for(Project project: ProjectManager.getInstance().getOpenProjects()) {
            if(name.equals(project.getName())) {
                return project;
            }
        }

        return null;
    }

    public void handleProject(Project project, HttpExchange xchg) throws IOException {

        String content = StreamUtil.readText(xchg.getRequestBody(), "UTF-8");

        JsonParser p = new JsonParser();

        JsonObject result = p.parse(content).getAsJsonObject();
        RemoteStorage instance = RemoteStorage.getInstance(project);
        instance.setJson(result);

        for(Map.Entry<String, JsonElement> providerKey : result.entrySet()) {
            for(Class provider: RemoteUtil.getProviderClasses()) {
                ProviderInterface providerInstance = instance.get(provider);
                if(providerInstance != null) {
                    if(providerInstance.getAlias().equals(providerKey.getKey())) {
                        providerInstance.collect(providerKey.getValue());
                    }
                }
            }
        }

        HttpExchangeUtil.sendResponse(xchg, "OK!!!");

    }

}
