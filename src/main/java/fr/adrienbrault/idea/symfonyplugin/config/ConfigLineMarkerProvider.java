package fr.adrienbrault.idea.symfonyplugin.config;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfonyplugin.Symfony2Icons;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.config.utils.ConfigUtil;
import fr.adrienbrault.idea.symfonyplugin.config.yaml.YamlElementPatternHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConfigLineMarkerProvider implements LineMarkerProvider {
    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> psiElements, @NotNull Collection<LineMarkerInfo> result) {
        if(psiElements.size() == 0 || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        LazyConfigTreeSignatures function = null;

        for (PsiElement psiElement : psiElements) {
            if(psiElement.getNode().getElementType() == YAMLTokenTypes.SCALAR_KEY && YamlElementPatternHelper.getRootConfigKeyPattern().accepts(psiElement)) {
                if(function == null) {
                    function = new LazyConfigTreeSignatures(psiElements.get(0).getProject());
                }
                visitRootElements(result, psiElement, function);
            }
        }
    }

    private void visitRootElements(@NotNull Collection<LineMarkerInfo> result, @NotNull PsiElement psiElement, @NotNull LazyConfigTreeSignatures function) {
        PsiElement parent = psiElement.getParent();
        if(!(parent instanceof YAMLKeyValue)) {
            return;
        }

        String keyText = ((YAMLKeyValue) parent).getKeyText();
        Map<String, Collection<String>> treeSignatures = function.value();
        if(!treeSignatures.containsKey(keyText)) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
            .setTargets(new MyClassIdLazyValue(psiElement.getProject(), treeSignatures.get(keyText), keyText))
            .setTooltipText("Navigate to configuration");

        result.add(builder.createLineMarkerInfo(psiElement));
    }

    private static class MyClassIdLazyValue extends NotNullLazyValue<Collection<? extends PsiElement>> {
        @NotNull
        private final Project project;

        @NotNull
        private final Collection<String> configuration;

        @NotNull
        private final String root;

        MyClassIdLazyValue(@NotNull Project project, @NotNull Collection<String> configuration, @NotNull String root) {
            this.project = project;
            this.configuration = configuration;
            this.root = root;
        }

        @NotNull
        @Override
        protected Collection<? extends PsiElement> compute() {
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
            if(this.treeSignatures == null) {
                return this.treeSignatures = ConfigUtil.getTreeSignatures(project);
            }

            return this.treeSignatures;
        }
    }
}
