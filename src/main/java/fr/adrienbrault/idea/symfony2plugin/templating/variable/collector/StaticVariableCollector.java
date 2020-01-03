package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provide support for Twig global "app".
 *
 * Its just a static fallback if no other implementation is able to catch up
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class StaticVariableCollector implements TwigFileVariableCollector {
    private static final HashSet<String> APP_CLASSES = new HashSet<>(Arrays.asList(
        "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\GlobalVariables", // valid inside Symfony < 5
        "\\Symfony\\Bridge\\Twig\\AppVariable"
    ));

    @Override
    public void collect(@NotNull TwigFileVariableCollectorParameter parameter, @NotNull Map<String, Set<String>> variables) {
        variables.put("app", new HashSet<>(APP_CLASSES)); // const must be a copy
    }

    public static boolean isUserMethod(@NotNull Method method) {
        return APP_CLASSES.stream()
            .anyMatch(phpClass -> PhpElementsUtil.isMethodInstanceOf(method, phpClass, "getUser"));
    }
}
