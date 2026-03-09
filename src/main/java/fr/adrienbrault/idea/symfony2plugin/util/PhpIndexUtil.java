package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpClassFqnIndex;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpInterfaceFqnIndex;
import com.jetbrains.php.util.PhpContractUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpIndexUtil {
    /**
     * Collect PhpClass which are inside current namespace and in sub-namespaces
     *
     * @param project current project
     * @param namespaceName namespace name should start with \ and end with "\"
     * @return classes inside namespace and sub-namespace
     */
    @NotNull
    public static Collection<PhpClass> getPhpClassInsideNamespace(@NotNull Project project, @NotNull String namespaceName) {
        return getPhpClassInsideNamespace(project, PhpIndex.getInstance(project), namespaceName, false);
    }

    /**
     * Collect PhpClass which are inside current namespace and in sub-namespaces
     *
     * @param project current project
     * @param namespaceName namespace name should start with \ and end with "\"
     * @return classes inside namespace and sub-namespace
     */
    @NotNull
    public static Collection<PhpClass> getPhpClassInsideNamespace(@NotNull Project project, @NotNull String namespaceName, boolean excludeInterfaces) {
        return getPhpClassInsideNamespace(project, PhpIndex.getInstance(project), namespaceName, excludeInterfaces);
    }

    public static Collection<PhpClass> getAllSubclasses(@NotNull Project project, @NotNull String clazz) {
        Collection<PhpClass> phpClasses = new ArrayList<>();

        PhpIndex.getInstance(project).processAllSubclasses(clazz, phpClass -> {
            phpClasses.add(phpClass);
            return true;
        });

        return phpClasses;
    }


    @NotNull
    private static Collection<PhpClass> getPhpClassInsideNamespace(@NotNull Project project, @NotNull PhpIndex phpIndex, @NotNull String namespaceName, boolean excludeInterfaces) {
        PhpContractUtil.assertFqn(namespaceName);
        Set<String> classes = new HashSet<>();

        FileBasedIndex.getInstance().processAllKeys(PhpClassFqnIndex.KEY, fqn -> {
            if (fqn.startsWith(namespaceName)) {
                classes.add(fqn);
            }
            return true;
        }, project);

        if (!excludeInterfaces) {
            FileBasedIndex.getInstance().processAllKeys(PhpInterfaceFqnIndex.KEY, fqn -> {
                if (fqn.startsWith(namespaceName)) {
                    classes.add(fqn);
                }
                return true;
            }, project);
        }

        Collection<PhpClass> clazzes = new HashSet<>();
        for (String s : classes) {
            clazzes.addAll(phpIndex.getAnyByFQN(s));
        }

        return clazzes;
    }

    public static boolean hasNamespace(@NotNull Project project, @NotNull String namespaceName) {

        if(!namespaceName.startsWith("\\")) {
            namespaceName = "\\" + namespaceName;
        }

        return !PhpIndex.getInstance(project).getChildNamespacesByParentName(namespaceName + "\\").isEmpty();
    }
}
