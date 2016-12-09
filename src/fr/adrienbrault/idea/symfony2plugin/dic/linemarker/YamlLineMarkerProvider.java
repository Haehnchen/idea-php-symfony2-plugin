package fr.adrienbrault.idea.symfony2plugin.dic.linemarker;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    private Map<String, Collection<ContainerService>> decoratedServiceCache;

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

        // services -> service_name
        psiElements.stream()
            .filter(psiElement -> psiElement instanceof YAMLKeyValue && YamlElementPatternHelper.getServiceIdKeyValuePattern().accepts(psiElement))
            .forEach(psiElement -> visitServiceId((YAMLKeyValue) psiElement, result));

        decoratedServiceCache = null;
    }

    private void visitServiceId(@NotNull YAMLKeyValue yamlKeyValue, @NotNull Collection<LineMarkerInfo> result) {
        String id = yamlKeyValue.getKeyText();
        if(StringUtils.isBlank(id)) {
            return;
        }

        // decorates: @foobar
        String decorates = YamlHelper.getYamlKeyValueAsString(yamlKeyValue, "decorates");
        if(StringUtils.isNotBlank(decorates)) {
            result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(yamlKeyValue, decorates, result));
        }

        if(this.decoratedServiceCache == null) {
            this.decoratedServiceCache = ServiceIndexUtil.getDecoratedServices(yamlKeyValue.getProject());
        }

        NavigationGutterIconBuilder<PsiElement> lineMarker = ServiceUtil.getLineMarkerForDecoratedServiceId(
            yamlKeyValue.getProject(), this.decoratedServiceCache, id
        );

        if(lineMarker == null) {
            return;
        }

        result.add(lineMarker.createLineMarkerInfo(yamlKeyValue));
    }
}
