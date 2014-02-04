package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;

import java.util.HashMap;
import java.util.Set;

public class ControllerVariableCollector implements TwigFileVariableCollector, TwigFileVariableCollector.TwigFileVariableCollectorExt {

    @Override
    public void collect(TwigFileVariableCollectorParameter parameter, HashMap<String, Set<String>> variables) {
        //variables.putAll(TwigUtil.collectControllerTemplateVariables(parameter.getElement()));
    }

    @Override
    public void collectVars(TwigFileVariableCollectorParameter parameter, HashMap<String, PsiVariable> variables) {
        variables.putAll(TwigUtil.collectControllerTemplateVariables(parameter.getElement()));
    }

}
