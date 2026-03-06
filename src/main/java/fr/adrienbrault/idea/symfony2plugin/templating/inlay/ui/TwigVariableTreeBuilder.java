package fr.adrienbrault.idea.symfony2plugin.templating.inlay.ui;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Builds Level-2 property nodes for the Twig variable tree popup.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigVariableTreeBuilder {

    @NotNull
    public static List<TwigVariableTreeNode> buildProperties(
        @NotNull Project project,
        @NotNull String parentVarName,
        @NotNull Set<String> types
    ) {
        Map<String, Set<String>> propMap = new TreeMap<>();

        for (String type : types) {
            String baseType = type.replaceAll("\\[]$", "");
            if (baseType.isBlank() || !baseType.startsWith("\\")) continue;

            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, baseType);
            if (phpClass == null) continue;

            for (Method method : phpClass.getMethods()) {
                if (!TwigTypeResolveUtil.isTwigAccessibleMethod(method)) continue;
                String propName = TwigTypeResolveUtil.getPropertyShortcutMethodName(method);
                resolveCleanTypes(project, method.getType())
                    .forEach(t -> propMap.computeIfAbsent(propName, k -> new LinkedHashSet<>()).add(t));
            }

            for (Field field : phpClass.getFields()) {
                if (!field.getModifier().isPublic() || field.getModifier().isStatic()) continue;
                resolveCleanTypes(project, field.getType())
                    .forEach(t -> propMap.computeIfAbsent(field.getName(), k -> new LinkedHashSet<>()).add(t));
            }
        }

        List<TwigVariableTreeNode> result = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : propMap.entrySet()) {
            result.add(new TwigVariableTreeNode(entry.getKey(), entry.getValue(), parentVarName));
        }
        return result;
    }

    private static Collection<String> resolveCleanTypes(@NotNull Project project, @NotNull PhpType rawType) {
        PhpType resolved = PhpIndex.getInstance(project).completeType(project, rawType, new HashSet<>());
        List<String> result = new ArrayList<>();
        for (String t : resolved.getTypes()) {
            if (!t.isBlank() && !PhpType.isUnresolved(t)) {
                result.add(t);
            }
        }
        return result;
    }
}
