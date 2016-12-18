package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;

import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface TwigFileVariableCollector {

    void collect(TwigFileVariableCollectorParameter parameter, Map<String, Set<String>> variables);

    interface TwigFileVariableCollectorExt {
        void collectVars(TwigFileVariableCollectorParameter parameter, Map<String, PsiVariable> variables);
    }

}
