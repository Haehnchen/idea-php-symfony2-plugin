package fr.adrienbrault.idea.symfony2plugin.routing;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class Route {

    private String name;
    private String controller;

    private HashMap<String, String> variables;
    private HashMap<String, String> defaults;
    private HashMap<String, String> requirements;
    private ArrayList<Collection<String>> tokens;

    public Route(String name, HashMap<String, String> variables, HashMap<String, String> defaults, HashMap<String, String> requirements, ArrayList<Collection<String>> tokens) {
        this.name = name;

        this.variables = variables;
        this.defaults = defaults;
        this.requirements = requirements;
        this.tokens = tokens;

        if(defaults.containsKey("_controller")) {
            this.controller = defaults.get("_controller").replace("\\\\", "\\");
        }
    }

    public Route(String name, String controller) {
        this.name = name;
        this.controller = controller;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getController() {
        return controller;
    }

    public HashMap<String, String> getVariables() {
        return variables;
    }

    public HashMap<String, String> getDefaults() {
        return defaults;
    }

    public HashMap<String, String> getRequirements() {
        return requirements;
    }

    public ArrayList<Collection<String>> getTokens() {
        return tokens;
    }

}
