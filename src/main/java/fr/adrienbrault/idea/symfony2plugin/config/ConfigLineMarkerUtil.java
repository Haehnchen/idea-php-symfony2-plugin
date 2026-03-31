package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.config.utils.ConfigUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
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

    private record ConfigTargetSupplier(@NotNull Project project, @NotNull Collection<String> configuration, @NotNull String root) implements Supplier<Collection<? extends PsiElement>> {
        @Override
        public Collection<? extends PsiElement> get() {
            return ConfigUtil.getTreeSignatureTargets(project, root, configuration);
        }
    }
}
