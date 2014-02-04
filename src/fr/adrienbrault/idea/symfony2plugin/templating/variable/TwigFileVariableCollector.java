package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;

import java.util.HashMap;
import java.util.Set;

public interface TwigFileVariableCollector {

    public void collect(TwigFileVariableCollectorParameter parameter, HashMap<String, Set<String>> variables);

    public interface TwigFileVariableCollectorExt {
        public void collectVars(TwigFileVariableCollectorParameter parameter, HashMap<String, PsiVariable> variables);
    }

}
