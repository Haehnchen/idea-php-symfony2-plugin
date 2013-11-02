package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import java.util.HashMap;
import java.util.Set;

public interface TwigFileVariableCollector {

    public void collect(TwigFileVariableCollectorParameter parameter, HashMap<String, Set<String>> variables);

}
