package fr.adrienbrault.idea.symfonyplugin.templating.variable;

import fr.adrienbrault.idea.symfonyplugin.templating.variable.dict.PsiVariable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface TwigFileVariableCollector {

    default void collect(@NotNull TwigFileVariableCollectorParameter parameter, @NotNull Map<String, Set<String>> variables) {}

    default void collectPsiVariables(@NotNull TwigFileVariableCollectorParameter parameter, @NotNull Map<String, PsiVariable> variables) {}
}
