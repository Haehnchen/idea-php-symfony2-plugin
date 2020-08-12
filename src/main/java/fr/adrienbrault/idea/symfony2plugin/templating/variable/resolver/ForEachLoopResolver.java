package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Provide the loop.XXX types inside foreach block if previous element is named loop with the type "\loop" {% for ... %}{% endfor %}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ForEachLoopResolver implements TwigTypeResolver {

    /**
     * @link https://twig.symfony.com/doc/3.x/tags/for.html#the-loop-variable
     */
    public static final String[] LOOP_VARIABLES = {
        "index", "index0", "revindex", "revindex0", "first", "last", "length", "parent"
    };

    @Override
    public void resolve(Collection<TwigTypeContainer> targets, @Nullable Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable Collection<PsiVariable> psiVariables) {
        if (previousElement == null || previousElement.stream().noneMatch(p -> "\\loop".equals(p.getStringElement()))) {
            return;
        }

        for(String string: LOOP_VARIABLES) {
            targets.add(new TwigTypeContainer(string));
        }
    }
}
