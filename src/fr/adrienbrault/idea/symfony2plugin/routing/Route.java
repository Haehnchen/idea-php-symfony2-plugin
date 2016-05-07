package fr.adrienbrault.idea.symfony2plugin.routing;

import fr.adrienbrault.idea.symfony2plugin.routing.dict.RouteInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class Route implements RouteInterface {

    @NotNull
    final private String name;

    @NotNull
    private Collection<String> methods = new HashSet<>();
    private String controller;
    private String path;
    private Set<String> pathCache;

    private HashSet<String> variables = new HashSet<String>();
    private HashMap<String, String> defaults = new HashMap<String, String>();
    private HashMap<String, String> requirements = new HashMap<String, String>();
    private List<Collection<String>> tokens = new ArrayList<Collection<String>>();

    public Route(@NotNull String name, HashSet<String> variables, HashMap<String, String> defaults, HashMap<String, String> requirements, ArrayList<Collection<String>> tokens) {
        this.name = name;

        this.variables = variables;
        this.defaults = defaults;
        this.requirements = requirements;
        this.tokens = tokens;

        if(defaults.containsKey("_controller")) {
            this.controller = defaults.get("_controller").replace("\\\\", "\\");
        }
    }
    public Route(@NotNull String name) {
        this.name = name;
    }

    public Route(@NotNull String name, @NotNull String controller) {
        this.name = name;
        this.controller = controller;
    }

    public Route(@NotNull RouteInterface routeInterface) {
        this.name = routeInterface.getName();
        this.controller = routeInterface.getController();
        this.path = routeInterface.getPath();
        this.methods = routeInterface.getMethods();
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getController() {
        return controller;
    }

    @NotNull
    public Set<String> getVariables() {

        if(this.path == null) {
            return variables;
        }

        if(this.pathCache != null) {
            return this.pathCache;
        }

        // possible fallback
        // /hello/{foo}/{foo1}/bar
        Set<String> hashSet = new TreeSet<String>();
        Matcher matcher = Pattern.compile("\\{(\\w+)}").matcher(this.path);
        while(matcher.find()){
            hashSet.add(matcher.group(1));
        }

        return this.pathCache = hashSet;
    }

    public Map<String, String> getDefaults() {
        return defaults;
    }

    public Map<String, String> getRequirements() {
        return requirements;
    }

    public List<Collection<String>> getTokens() {
        return tokens;
    }

    @Nullable
    public String getPath() {
        return path;
    }

    @NotNull
    @Override
    public Collection<String> getMethods() {
        return this.methods;
    }
}
