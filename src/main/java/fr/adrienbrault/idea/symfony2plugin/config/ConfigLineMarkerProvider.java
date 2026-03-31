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
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLValue;

import java.util.Collection;
import java.util.List;

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
        if(psiElements.isEmpty()) {
            return;
        }

        Project project = psiElements.get(0).getProject();
        if (!Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        ConfigLineMarkerUtil.LazyConfigTreeSignatures function = null;
        var rootConfigKeyPattern = YamlElementPatternHelper.getRootConfigKeyPattern();

        var resourcePattern = YamlElementPatternHelper.getSingleLineScalarKey("resource");

        for (PsiElement psiElement : psiElements) {
            if(psiElement.getNode().getElementType() == YAMLTokenTypes.SCALAR_KEY && rootConfigKeyPattern.accepts(psiElement)) {
                if(function == null) {
                    function = new ConfigLineMarkerUtil.LazyConfigTreeSignatures(project);
                }
                visitRootElements(project, result, psiElement, function);
            } else if (resourcePattern.accepts(psiElement)) {
                visitResourceValue(project, result, psiElement);
            }
        }
    }

    private void visitRootElements(@NotNull Project project, @NotNull Collection<? super LineMarkerInfo<?>> result, @NotNull PsiElement psiElement, @NotNull ConfigLineMarkerUtil.LazyConfigTreeSignatures function) {
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

    private void visitConfigKey(@NotNull Project project, @NotNull Collection<? super LineMarkerInfo<?>> result, @NotNull PsiElement psiElement, @NotNull ConfigLineMarkerUtil.LazyConfigTreeSignatures function, String keyText) {
        LineMarkerInfo<PsiElement> marker = ConfigLineMarkerUtil.createConfigNavigationMarker(project, psiElement, function, keyText);
        if (marker != null) {
            result.add(marker);
        }
    }

    private void visitResourceValue(@NotNull Project project, @NotNull Collection<? super LineMarkerInfo<?>> result, @NotNull PsiElement psiElement) {
        String resourcePath = PsiElementUtils.trimQuote(psiElement.getText());

        if (!FileResourceUtil.hasFileResourceTargets(project, psiElement.getContainingFile(), resourcePath)) {
            return;
        }

        result.add(NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
            .setTargets(NotNullLazyValue.lazy(() -> FileResourceUtil.getFileResourceTargets(project, psiElement.getContainingFile(), resourcePath)))
            .setTooltipText("Navigate to resource")
            .createLineMarkerInfo(psiElement));
    }
}
