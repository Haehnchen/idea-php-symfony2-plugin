package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;

import java.util.*;

public class StaticVariableCollector implements TwigFileVariableCollector {

    @Override
    public void collect(TwigFileVariableCollectorParameter parameter, Map<String, Set<String>> variables) {
        variables.put("app",  new HashSet<String>(Arrays.asList("\\Symfony\\Bundle\\FrameworkBundle\\Templating\\GlobalVariables")));
    }

}
