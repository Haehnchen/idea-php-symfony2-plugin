package fr.adrienbrault.idea.symfony2plugin.routing;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class Route {

    private String name;
    private String controller;

    public Route(String name, String controller) {
        this.name = name;
        this.controller = controller;
    }

    public String getName() {
        return name;
    }

    public String getController() {
        return controller;
    }
}
