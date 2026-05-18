package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Provides Twig's special "loop" variable inside for loops.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ForLoopVariableResolver implements TwigTypeResolver {

    // Twig creates the $context['loop'] array in \Twig\Node\ForNode::compile.
    public static final String LOOP_VARIABLE_TYPE = "\\loop";

    // Twig writes these keys in \Twig\Node\ForNode::compile and updates them in \Twig\Node\ForLoopNode::compile.
    public static final String[] LOOP_VARIABLES = {
        "index",
        "index0",
        "revindex",
        "revindex0",
        "first",
        "last",
        "length",
        "parent",
    };

    @Override
    public void resolve(@NotNull Project project, Collection<TwigTypeContainer> targets, @Nullable Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable Collection<PsiVariable> psiVariables) {
        if (previousElement == null || previousElement.stream().noneMatch(ForLoopVariableResolver::isLoopVariableType)) {
            return;
        }

        for (String loopVariable : LOOP_VARIABLES) {
            targets.add(new TwigTypeContainer(loopVariable));
        }
    }

    private static boolean isLoopVariableType(@NotNull TwigTypeContainer twigTypeContainer) {
        return twigTypeContainer.getTypes().contains(LOOP_VARIABLE_TYPE);
    }
}
