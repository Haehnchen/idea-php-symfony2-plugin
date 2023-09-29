package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.util.PhpContractUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
        return getPhpClassInsideNamespace(PhpIndex.getInstance(project), namespaceName, 10);
    }

    @NotNull
    private static Collection<PhpClass> getPhpClassInsideNamespace(@NotNull PhpIndex phpIndex, @NotNull String namespaceName, int maxDeep) {
        PhpContractUtil.assertFqn(namespaceName);

        Collection<String> classes = new HashSet<>() {{
            addAll(phpIndex.getAllClassFqns(PrefixMatcher.ALWAYS_TRUE));
            addAll(phpIndex.getAllInterfacesFqns(PrefixMatcher.ALWAYS_TRUE));
        }};

        Set<String> stringStream = classes.stream()
            .filter(s -> s.toLowerCase().startsWith(StringUtils.stripEnd(namespaceName.toLowerCase(), "\\") + "\\"))
            .collect(Collectors.toSet());

        Collection<PhpClass> clazzes = new HashSet<>();
        for (String s : stringStream) {
            clazzes.addAll(phpIndex.getAnyByFQN(s));
        }

        return clazzes;
    }

    public static boolean hasNamespace(@NotNull Project project, @NotNull String namespaceName) {

        if(!namespaceName.startsWith("\\")) {
            namespaceName = "\\" + namespaceName;
        }

        return PhpIndex.getInstance(project).getChildNamespacesByParentName(namespaceName + "\\").size() > 0;
    }
}
