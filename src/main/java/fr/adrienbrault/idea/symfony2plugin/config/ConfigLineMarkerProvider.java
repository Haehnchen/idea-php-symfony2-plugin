package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.utils.ConfigUtil;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLValue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConfigLineMarkerProvider implements LineMarkerProvider {
    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> psiElements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        if(psiElements.size() == 0) {
            return;
        }

        Project project = psiElements.get(0).getProject();
        if (!Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        LazyConfigTreeSignatures function = null;

        for (PsiElement psiElement : psiElements) {
            if(psiElement.getNode().getElementType() == YAMLTokenTypes.SCALAR_KEY && YamlElementPatternHelper.getRootConfigKeyPattern().accepts(psiElement)) {
                if(function == null) {
                    function = new LazyConfigTreeSignatures(project);
                }
                visitRootElements(project, result, psiElement, function);
            }
        }
    }

    private void visitRootElements(@NotNull Project project, @NotNull Collection<? super LineMarkerInfo<?>> result, @NotNull PsiElement psiElement, @NotNull LazyConfigTreeSignatures function) {
        PsiElement parent = psiElement.getParent();
        if(!(parent instanceof YAMLKeyValue)) {
            return;
        }

        String keyText = ((YAMLKeyValue) parent).getKeyText();

        // nested condition:
        // when@prod:
        //    framework:
        //        router: ~
        if (keyText.startsWith("when@")) {
            YAMLValue value = ((YAMLKeyValue) parent).getValue();

            for (YAMLKeyValue yamlKeyValue : PsiTreeUtil.getChildrenOfTypeAsList(value, YAMLKeyValue.class)) {
                String keyText1 = yamlKeyValue.getKeyText();
                if (StringUtils.isNotBlank(keyText1)) {
                    PsiElement key = yamlKeyValue.getKey();
                    if (key != null) {
                        visitConfigKey(project, result, key, function, keyText1);
                    }
                }
            }

            return;
        }

        visitConfigKey(project, result, psiElement, function, keyText);
    }

    private void visitConfigKey(@NotNull Project project, @NotNull Collection<? super LineMarkerInfo<?>> result, @NotNull PsiElement psiElement, @NotNull LazyConfigTreeSignatures function, String keyText) {
        Map<String, Collection<String>> treeSignatures = function.value();
        if(!treeSignatures.containsKey(keyText)) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
            .setTargets(NotNullLazyValue.lazy(new MyClassIdLazyValue(project, treeSignatures.get(keyText), keyText)))
            .setTooltipText("Navigate to configuration");

        result.add(builder.createLineMarkerInfo(psiElement));
    }

    private record MyClassIdLazyValue(@NotNull Project project, @NotNull Collection<String> configuration, @NotNull String root) implements Supplier<Collection<? extends PsiElement>> {
        @Override
        public Collection<? extends PsiElement> get() {
            return ConfigUtil.getTreeSignatureTargets(project, root, configuration);
        }
    }

    private static class LazyConfigTreeSignatures {
        @NotNull
        private final Project project;

        private Map<String, Collection<String>> treeSignatures;

        LazyConfigTreeSignatures(@NotNull Project project) {
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
}
