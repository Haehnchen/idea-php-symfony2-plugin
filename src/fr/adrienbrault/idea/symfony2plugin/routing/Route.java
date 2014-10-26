package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class Route {

    private String name;
    private String controller;
    private String path;
    private Set<String> pathCache;

    private HashSet<String> variables = new HashSet<String>();
    private HashMap<String, String> defaults = new HashMap<String, String>();
    private HashMap<String, String> requirements = new HashMap<String, String>();
    private List<Collection<String>> tokens = new ArrayList<Collection<String>>();

    public Route(String name, HashSet<String> variables, HashMap<String, String> defaults, HashMap<String, String> requirements, ArrayList<Collection<String>> tokens) {
        this.name = name;

        this.variables = variables;
        this.defaults = defaults;
        this.requirements = requirements;
        this.tokens = tokens;

        if(defaults.containsKey("_controller")) {
            this.controller = defaults.get("_controller").replace("\\\\", "\\");
        }
    }
    public Route(String name) {
        this.name = name;
    }

    public Route(String name, String controller) {
        this.name = name;
        this.controller = controller;
    }

    public Route(String name, String[] indexed) {
        this.name = name;
        if(indexed.length >= 1 && indexed[0].length() > 0) {
            this.controller = indexed[0];
        }

        if(indexed.length >= 2 && indexed[1].length() > 0) {
            this.path = indexed[1];
        }

    }

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
        Set<String> hashSet = new HashSet<String>();
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

}
