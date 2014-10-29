package fr.adrienbrault.idea.symfony2plugin.remote.provider;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import java.util.*;

public class RouterProvider implements ProviderInterface {

    private Map<String, Route> routes = new HashMap<String, Route>();

    public void collect(JsonElement jsonElement) {

        this.routes = new HashMap<String, Route>();

        if(!jsonElement.getAsJsonObject().has("routes")) {
            return;
        }

        for(JsonElement route : jsonElement.getAsJsonObject().get("routes").getAsJsonArray()) {

            JsonObject jsonObject = route.getAsJsonObject();
            if(!jsonObject.has("name")) {
                continue;
            }

            String name = jsonObject.get("name").getAsString();
            if(jsonObject.has("controller")) {
                this.routes.put(name, new Route(name, jsonObject.get("controller").getAsString()));
            } else {
                this.routes.put(name, new Route(name));
            }

        }

    }

    public Map<String, Route> getRoutes() {
        return routes;
    }

    public String getAlias() {
        return "router";
    }


}
