package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.text.VersionComparatorUtil;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

import static org.apache.commons.lang.StringUtils.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyUtil {
    private static boolean compare(@NotNull Project project, @NotNull String version, @NotNull Comparator comparator) {
        for (PhpClass phpClass : PhpElementsUtil.getClassesInterface(project, "Symfony\\Component\\HttpKernel\\Kernel")) {
            Field versionField = phpClass.findFieldByName("VERSION", true);
            if(versionField == null) {
                continue;
            }

            PsiElement defaultValue = versionField.getDefaultValue();
            if(!(defaultValue instanceof StringLiteralExpression)) {
                continue;
            }

            String contents = ((StringLiteralExpression) defaultValue).getContents();
            if(isBlank(contents)) {
                continue;
            }

            // 3.2.0-DEV
            contents = stripEnd(contents.toLowerCase(), "dev");
            contents = stripEnd(contents, "-");

            if(comparator.accepts(contents)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isVersionGreaterThenEquals(@NotNull Project project, @NotNull String version) {
        return compare(project, version, contents -> VersionComparatorUtil.compare(contents, version) >= 0);
    }

    public static boolean isVersionGreaterThen(@NotNull Project project, @NotNull String version) {
        return compare(project, version, contents -> VersionComparatorUtil.compare(contents, version) > 0);
    }

    public static boolean isVersionLessThenEquals(@NotNull Project project, @NotNull String version) {
        return compare(project, version, contents -> VersionComparatorUtil.compare(contents, version) <= 0);
    }

    public static boolean isVersionLessThen(@NotNull Project project, @NotNull String version) {
        return compare(project, version, contents -> VersionComparatorUtil.compare(contents, version) < 0);
    }

    private interface Comparator {
        boolean accepts(@NotNull String contents);
    }
}
