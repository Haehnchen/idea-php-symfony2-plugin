package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpClassFqnIndex;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpInterfaceFqnIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides Twig's special "loop" variable inside for loops.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ForLoopVariableResolver implements TwigTypeResolver {

    // Twig creates the $context['loop'] array in \Twig\Node\ForNode::compile.
    public static final String LOOP_VARIABLE_TYPE = "\\loop";
    public static final String TWIG4_LOOP_CONTEXT_FQN = "\\Twig\\Runtime\\LoopContext";
    private static final Key<CachedValue<Collection<String>>> LOOP_VARIABLES_CACHE = new Key<>("SYMFONY_TWIG_LOOP_VARIABLES");

    // Twig writes these keys in \Twig\Node\ForNode::compile and updates them in \Twig\Node\ForLoopNode::compile.
    public static final String[] TWIG3_LOOP_VARIABLES = {
        "index",
        "index0",
        "revindex",
        "revindex0",
        "first",
        "last",
        "length",
        "parent",
    };

    @NotNull
    public static Collection<String> getLoopVariables(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            LOOP_VARIABLES_CACHE,
            () -> CachedValueProvider.Result.create(
                getLoopVariablesInner(project),
                FileIndexCaches.getModificationTrackerForIndexId(project, PhpClassFqnIndex.KEY),
                FileIndexCaches.getModificationTrackerForIndexId(project, PhpInterfaceFqnIndex.KEY)
            ),
            false
        );
    }

    @Override
    public void resolve(@NotNull Project project, Collection<TwigTypeContainer> targets, @Nullable Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable Collection<PsiVariable> psiVariables) {
        if (previousElement == null || previousElement.stream().noneMatch(ForLoopVariableResolver::isLoopVariableType)) {
            return;
        }

        for (String loopVariable : getLoopVariables(project)) {
            targets.add(new TwigTypeContainer(loopVariable));
        }
    }

    @NotNull
    private static Collection<String> getLoopVariablesInner(@NotNull Project project) {
        PhpClass loopContext = PhpElementsUtil.getClassInterface(project, TWIG4_LOOP_CONTEXT_FQN);
        if (loopContext == null) {
            return Arrays.asList(TWIG3_LOOP_VARIABLES);
        }

        Set<String> variables = new LinkedHashSet<>();
        for (Method method : loopContext.getMethods()) {
            if (!TwigTypeResolveUtil.isTwigAccessibleMethod(method)) {
                continue;
            }

            variables.add(TwigTypeResolveUtil.getPropertyShortcutMethodName(method));
        }

        return variables;
    }

    private static boolean isLoopVariableType(@NotNull TwigTypeContainer twigTypeContainer) {
        return twigTypeContainer.getTypes().contains(LOOP_VARIABLE_TYPE);
    }
}
