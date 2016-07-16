package fr.adrienbrault.idea.symfony2plugin.routing.dict;

import fr.adrienbrault.idea.symfony2plugin.routing.Route;

import java.util.HashMap;
import java.util.Map;

public class RoutesContainer {

    private final Long lastMod;
    private Map<String, Route> routes = new HashMap<>();

    public RoutesContainer(Long lastMod, Map<String, Route> routes) {
        this(lastMod);
        this.routes = routes;
    }

    public RoutesContainer(Long lastMod) {
        this.lastMod = lastMod;
    }

    public Long getLastMod() {
        return lastMod;
    }

    public Map<String, Route> getRoutes() {
        return routes;
    }

    public RoutesContainer setRoutes(Map<String, Route> routes) {
        this.routes = routes;
        return this;
    }

}
