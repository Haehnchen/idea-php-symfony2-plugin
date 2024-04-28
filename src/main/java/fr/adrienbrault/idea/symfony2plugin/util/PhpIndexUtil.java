package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
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
        return getPhpClassInsideNamespace(PhpIndex.getInstance(project), namespaceName, false);
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
        return getPhpClassInsideNamespace(PhpIndex.getInstance(project), namespaceName, excludeInterfaces);
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
    private static Collection<PhpClass> getPhpClassInsideNamespace(@NotNull PhpIndex phpIndex, @NotNull String namespaceName, boolean excludeInterfaces) {
        PhpContractUtil.assertFqn(namespaceName);

        Collection<String> classes = new HashSet<>(phpIndex.getAllClassFqns(new MyPrefixMatcher(namespaceName)));
        if (!excludeInterfaces) {
            classes.addAll(phpIndex.getAllInterfacesFqns(new MyPrefixMatcher(namespaceName)));
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

    private static class MyPrefixMatcher extends PrefixMatcher {
        private final String namespaceName;

        public MyPrefixMatcher(@NotNull String namespaceName) {
            super(namespaceName);
            this.namespaceName = namespaceName;
        }

        @Override
        public boolean prefixMatches(@NotNull String name) {
            return name.startsWith(namespaceName);
        }

        @Override
        public @NotNull PrefixMatcher cloneWithPrefix(@NotNull String prefix) {
            return new MyPrefixMatcher(prefix);
        }
    }
}
