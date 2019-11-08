package fr.adrienbrault.idea.symfony2plugin.dic.linemarker;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
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
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> psiElements, @NotNull Collection<LineMarkerInfo> result) {
        if(psiElements.size() == 0 || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        LazyDecoratedParentServiceValues lazyDecoratedServices = null;


        for (PsiElement psiElement : psiElements) {
            if(psiElement.getNode().getElementType() != YAMLTokenTypes.SCALAR_KEY) {
                continue;
            }

            PsiElement yamlKeyValue = psiElement.getParent();
            if(!(yamlKeyValue instanceof YAMLKeyValue) || !YamlElementPatternHelper.getServiceIdKeyValuePattern().accepts(yamlKeyValue)) {
                continue;
            }

            if(lazyDecoratedServices == null) {
                lazyDecoratedServices = new LazyDecoratedParentServiceValues(psiElement.getProject());
            }

            // services -> service_name
            visitServiceId(psiElement, (YAMLKeyValue) yamlKeyValue, result, lazyDecoratedServices);
        }
    }

    private void visitServiceId(@NotNull PsiElement leafTarget, @NotNull YAMLKeyValue yamlKeyValue, @NotNull Collection<LineMarkerInfo> result, @NotNull LazyDecoratedParentServiceValues lazyDecoratedServices) {
        String id = yamlKeyValue.getKeyText();
        if(StringUtils.isBlank(id)) {
            return;
        }

        // decorates: foobar
        String decorates = YamlHelper.getYamlKeyValueAsString(yamlKeyValue, "decorates");
        if(decorates != null && StringUtils.isNotBlank(decorates)) {
            result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(leafTarget, ServiceUtil.ServiceLineMarker.DECORATE, decorates));
        }

        // parent: foobar
        String parent = YamlHelper.getYamlKeyValueAsString(yamlKeyValue, "parent");
        if(parent != null && StringUtils.isNotBlank(parent)) {
            result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(leafTarget, ServiceUtil.ServiceLineMarker.PARENT, parent));
        }

        // foreign "decorates" linemarker
        NavigationGutterIconBuilder<PsiElement> decorateLineMarker = ServiceUtil.getLineMarkerForDecoratedServiceId(
            yamlKeyValue.getProject(),
            ServiceUtil.ServiceLineMarker.DECORATE,
            lazyDecoratedServices.getDecoratedServices(),
            id
        );

        if(decorateLineMarker != null) {
            result.add(decorateLineMarker.createLineMarkerInfo(leafTarget));
        }

        // foreign "parent" linemarker
        NavigationGutterIconBuilder<PsiElement> parentLineMarker = ServiceUtil.getLineMarkerForDecoratedServiceId(
            yamlKeyValue.getProject(),
            ServiceUtil.ServiceLineMarker.PARENT,
            lazyDecoratedServices.getDecoratedServices(),
            id
        );

        if(parentLineMarker != null) {
            result.add(parentLineMarker.createLineMarkerInfo(leafTarget));
        }
    }
}
