package fr.adrienbrault.idea.symfony2plugin.dic.linemarker;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceSerializable;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlLineMarkerProvider implements LineMarkerProvider {
    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> psiElements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        if (psiElements.isEmpty()) {
            return;
        }

        Project project = psiElements.get(0).getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        LazyDecoratedParentServiceValues lazyDecoratedServices = null;
        var serviceIdKeyValuePattern = YamlElementPatternHelper.getServiceIdKeyValuePattern();

        for (PsiElement psiElement : psiElements) {
            if(psiElement.getNode().getElementType() != YAMLTokenTypes.SCALAR_KEY) {
                continue;
            }

            PsiElement yamlKeyValue = psiElement.getParent();
            if(!(yamlKeyValue instanceof YAMLKeyValue) || !serviceIdKeyValuePattern.accepts(yamlKeyValue)) {
                continue;
            }

            if(lazyDecoratedServices == null) {
                lazyDecoratedServices = new LazyDecoratedParentServiceValues(project);
            }

            // services -> service_name
            visitServiceId(project, psiElement, (YAMLKeyValue) yamlKeyValue, result, lazyDecoratedServices);

            // services:
            //    App\:
            //        resource: '../src/*'
            visitServiceIdForResources(project, psiElement, (YAMLKeyValue) yamlKeyValue, result);
        }
    }

    private void visitServiceIdForResources(@NotNull Project project, PsiElement leafTarget, YAMLKeyValue yamlKeyValue, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        String id = yamlKeyValue.getKeyText();
        if (StringUtils.isBlank(id) || !id.endsWith("\\")) {
            return;
        }

        PsiFile containingFile = yamlKeyValue.getContainingFile();
        if (containingFile == null) {
            return;
        }

        VirtualFile containerFile = containingFile.getVirtualFile();
        if (containerFile == null) {
            return;
        }

        ServiceSerializable service = ServiceIndexUtil.findServiceDefinition(project, containerFile, id);
        if (service == null || service.getResource().isEmpty()) {
            return;
        }

        result.add(NavigationGutterIconBuilder.create(AllIcons.Modules.SourceRoot)
            .setTargets(NotNullLazyValue.lazy(() -> ServiceIndexUtil.getClassesForServiceDefinition(project, containerFile, service)))
            .setTooltipText("Navigate to class")
            .createLineMarkerInfo(leafTarget));
    }

    private void visitServiceId(@NotNull Project project, @NotNull PsiElement leafTarget, @NotNull YAMLKeyValue yamlKeyValue, @NotNull Collection<? super LineMarkerInfo<?>> result, @NotNull LazyDecoratedParentServiceValues lazyDecoratedServices) {
        String id = yamlKeyValue.getKeyText();
        if(StringUtils.isBlank(id)) {
            return;
        }

        ServiceLineMarkerUtil.collectDecoratesAndParentTargets(
            leafTarget,
            result,
            YamlHelper.getYamlKeyValueAsString(yamlKeyValue, "decorates"),
            YamlHelper.getYamlKeyValueAsString(yamlKeyValue, "parent")
        );

        ServiceLineMarkerUtil.collectDecoratedAndParentTargets(
            project,
            leafTarget,
            result,
            lazyDecoratedServices,
            id
        );
    }
}
