package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class StaticVariableCollector implements TwigFileVariableCollector {

    @Override
    public void collect(TwigFileVariableCollectorParameter parameter, Map<String, Set<String>> variables) {
        variables.put("app", new HashSet<>(Collections.singletonList("\\Symfony\\Bundle\\FrameworkBundle\\Templating\\GlobalVariables")));
    }

}
