package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility class for querying PhpAttributeIndex
 *
 * Index structure:
 * - Key: Attribute FQN (e.g., "\Twig\Attribute\AsTwigFilter")
 * - Value: List<String> where [0] = class FQN, [1+] = additional data
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpAttributeIndexUtil {
    /**
     * Get all indexed data for the given attribute FQN
     *
     * @param project      The project
     * @param attributeFqn The attribute FQN (e.g., "\Twig\Attribute\AsTwigFilter")
     * @return Collection of List<String> where [0] = class FQN, [1+] = additional data
     */
    @NotNull
    public static Collection<List<String>> getAttributeData(@NotNull Project project, @NotNull String attributeFqn) {
        return FileBasedIndex.getInstance().getValues(
                PhpAttributeIndex.KEY,
                attributeFqn,
                GlobalSearchScope.allScope(project)
        );
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
        PhpIndex phpIndex = PhpIndex.getInstance(project);

        for (List<String> data : getAttributeData(project, attributeFqn)) {
            if (!data.isEmpty()) {
                String classFqn = "\\" + data.get(0);
                Collection<PhpClass> foundClasses = phpIndex.getAnyByFQN(classFqn);
                if (!foundClasses.isEmpty()) {
                    classes.add(foundClasses.iterator().next());
                }
            }
        }

        return classes;
    }
}
