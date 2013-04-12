package fr.adrienbrault.idea.symfony2plugin.routing;

import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class Route {

    private String name;
    private Map<String, Object> defaults;

    public Route(String name, Map<String, Object> defaults) {
        this.name = name;
        this.defaults = defaults;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getDefaults() {
        return defaults;
    }
}
