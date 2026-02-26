package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.text.VersionComparatorUtil;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyUtil {

    private static final Key<CachedValue<Set<String>>> CACHE = new Key<>("SYMFONY_VERSION_CACHE");

    private static boolean compare(@NotNull Project project, @NotNull String version, @NotNull Comparator comparator) {
        Set<String> cache = CachedValuesManager.getManager(project).getCachedValue(
            project,
            CACHE,
            () -> CachedValueProvider.Result.create(getVersions(project), PsiModificationTracker.getInstance(project).forLanguage(PhpLanguage.INSTANCE)),
            false
        );

        for (String s : cache) {
            if(comparator.accepts(s)) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    public static Set<String> getVersions(@NotNull Project project) {
        Set<String> versions = new HashSet<>();

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

            // 3.2.0-DEV, 3.2.0-RC1
            contents = contents.toLowerCase().replaceAll("(.*)-([\\w]+)$", "$1");
            versions.add(contents);
        }

        return versions;
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
