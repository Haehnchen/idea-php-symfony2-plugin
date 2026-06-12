package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Utility class for querying PhpAttributeIndex
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpAttributeIndexUtil {
    /**
     * Get all indexed data for the given attribute FQN
     *
     * @param project      The project
     * @param attributeFqn The attribute FQN (e.g., "\Twig\Attribute\AsTwigFilter")
     * @return scoped targets for the given attribute FQN
     */
    @NotNull
    public static Collection<PhpAttributeIndex.AttributeTarget> getAttributeData(@NotNull Project project, @NotNull String attributeFqn) {
        return FileBasedIndex.getInstance()
            .getValues(
                PhpAttributeIndex.KEY,
                attributeFqn,
                GlobalSearchScope.allScope(project)
            )
            .stream()
            .flatMap(Collection::stream)
            .toList();
    }

    @NotNull
    public static Collection<PhpAttributeIndex.AttributeTarget> getTargetsWithAttributeScope(
        @NotNull Project project,
        @NotNull String attributeFqn,
        @NotNull PhpAttributeIndex.TargetScope scope
    ) {
        return getAttributeData(project, attributeFqn)
            .stream()
            .filter(target -> target.scope() == scope)
            .toList();
    }

    @NotNull
    public static Collection<PhpAttributeIndex.AttributeTarget> getMethodTargetsWithAttribute(@NotNull Project project, @NotNull String attributeFqn) {
        return getTargetsWithAttributeScope(project, attributeFqn, PhpAttributeIndex.TargetScope.METHOD);
    }

    /**
     * Get all PHP classes that have the given attribute
     *
     * @param project      The project
     * @param attributeFqn The attribute FQN
     * @return Collection of PhpClass instances
     */
    @NotNull
    public static Collection<PhpClass> getClassesWithAttribute(@NotNull Project project, @NotNull String attributeFqn) {
        Collection<PhpClass> classes = new ArrayList<>();

        for (PhpAttributeIndex.AttributeTarget target : getTargetsWithAttributeScope(project, attributeFqn, PhpAttributeIndex.TargetScope.PHP_CLASS)) {
            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, normalizeFqn(target.classFqn()));
            if (phpClass != null) {
                classes.add(phpClass);
            }
        }

        return classes;
    }

    @NotNull
    private static String normalizeFqn(@NotNull String fqn) {
        return fqn.startsWith("\\") ? fqn : "\\" + fqn;
    }
}
