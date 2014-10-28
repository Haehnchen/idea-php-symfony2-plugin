package fr.adrienbrault.idea.symfony2plugin.stubs.dict;

public class StubIndexedRoute {

    private final String name;
    private String controller = null;
    private String path = null;

    public StubIndexedRoute(String name) {
        this.name = name;
    }

    public String getController() {
        return controller;
    }

    public void setController(String controller) {
        this.controller = controller;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}