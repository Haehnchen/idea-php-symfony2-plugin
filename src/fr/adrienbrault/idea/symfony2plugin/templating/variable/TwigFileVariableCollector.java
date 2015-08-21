package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;

import java.util.Map;
import java.util.Set;

public interface TwigFileVariableCollector {

    public void collect(TwigFileVariableCollectorParameter parameter, Map<String, Set<String>> variables);

    public interface TwigFileVariableCollectorExt {
        public void collectVars(TwigFileVariableCollectorParameter parameter, Map<String, PsiVariable> variables);
    }

}
