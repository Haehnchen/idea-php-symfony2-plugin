package fr.adrienbrault.idea.symfony2plugin.routing;

import fr.adrienbrault.idea.symfony2plugin.routing.dict.RouteInterface;
import org.apache.commons.lang.StringUtils;
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

    private Set<String> variables = new HashSet<>();
    private Map<String, String> defaults = new HashMap<>();
    private Map<String, String> requirements = new HashMap<>();
    private List<Collection<String>> tokens = new ArrayList<>();

    public Route(@NotNull String name, @NotNull Set<String> variables, @NotNull Map<String, String> defaults, @NotNull Map<String, String> requirements, @NotNull List<Collection<String>> tokens) {
        this.name = name;

        this.variables = variables;
        this.defaults = defaults;
        this.requirements = requirements;
        this.tokens = tokens;

        if(defaults.containsKey("_controller")) {
            String controller = defaults.get("_controller");
            if(StringUtils.isNotBlank(controller)) {
                this.controller = controller.replace("\\\\", "\\");
            }
        }
    }

    public Route(@NotNull String name, @NotNull Set<String> variables, @NotNull Map<String, String> defaults, @NotNull Map<String, String> requirements, @NotNull List<Collection<String>> tokens, @Nullable String path) {
        this(name, variables, defaults, requirements, tokens);
        this.path = path;
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
        if (this.pathCache != null) {
            return this.pathCache;
        }

        Set<String> hashSet = new TreeSet<>(this.variables);
        if (this.path == null) {
            return this.pathCache = hashSet;
        }

        // possible fallback
        // /hello/{foo}/{foo1}/bar
        // /hello/{!foo}/{!foo<\d+>}
        // /hello/{foo<\d+>}
        Matcher matcher = Pattern.compile("\\{!?(\\w+)[<[^}].*>]*}").matcher(this.path);
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
