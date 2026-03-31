package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.config.utils.ConfigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class ConfigLineMarkerUtil {
    private ConfigLineMarkerUtil() {
    }

    @Nullable
    public static LineMarkerInfo<PsiElement> createConfigNavigationMarker(@NotNull Project project, @NotNull PsiElement psiElement, @NotNull LazyConfigTreeSignatures signatures, @NotNull String key) {
        Map<String, Collection<String>> treeSignatures = signatures.value();
        if (!treeSignatures.containsKey(key)) {
            return null;
        }

        return NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
            .setTargets(NotNullLazyValue.lazy(new ConfigTargetSupplier(project, treeSignatures.get(key), key)))
            .setTooltipText("Navigate to configuration")
            .createLineMarkerInfo(psiElement);
    }

    public static final class LazyConfigTreeSignatures {
        @NotNull
        private final Project project;

        private Map<String, Collection<String>> treeSignatures;

        public LazyConfigTreeSignatures(@NotNull Project project) {
            this.project = project;
        }

        @NotNull
        public Map<String, Collection<String>> value() {
            return Objects.requireNonNullElseGet(
                this.treeSignatures,
                () -> this.treeSignatures = ConfigUtil.getTreeSignatures(project)
            );
        }
    }

    /**
     * Resolve resource path targets for import statements.
     * Handles @Bundle paths and relative paths.
     *
     * @param containingFile the config file containing the resource reference (for relative path resolution)
     * @param resourcePath   the resource path string (e.g. "@FooBundle/config/services.yaml" or "../services.yml")
     * @return resolved targets, or empty collection if none found
     */
    @NotNull
    public static Collection<PsiElement> resolveResourceTargets(@NotNull Project project, @Nullable PsiFile containingFile, @NotNull String resourcePath) {
        if (StringUtils.isBlank(resourcePath)) {
            return Collections.emptyList();
        }

        Collection<PsiElement> targets = new ArrayList<>();
        if (resourcePath.startsWith("@")) {
            targets.addAll(FileResourceUtil.getFileResourceTargetsInBundleScope(project, resourcePath));
            targets.addAll(FileResourceUtil.getFileResourceTargetsInBundleDirectory(project, resourcePath));
        } else if (containingFile != null) {
            targets.addAll(FileResourceUtil.getFileResourceTargetsInDirectoryScope(containingFile, resourcePath));
        }

        return targets;
    }

    private record ConfigTargetSupplier(@NotNull Project project, @NotNull Collection<String> configuration, @NotNull String root) implements Supplier<Collection<? extends PsiElement>> {
        @Override
        public Collection<? extends PsiElement> get() {
            return ConfigUtil.getTreeSignatureTargets(project, root, configuration);
        }
    }
}
