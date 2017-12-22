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

        LazyDecoratedServiceValues lazyDecoratedServices = null;


        for (PsiElement psiElement : psiElements) {
            if(psiElement.getNode().getElementType() != YAMLTokenTypes.SCALAR_KEY) {
                continue;
            }

            PsiElement yamlKeyValue = psiElement.getParent();
            if(!(yamlKeyValue instanceof YAMLKeyValue) || !YamlElementPatternHelper.getServiceIdKeyValuePattern().accepts(yamlKeyValue)) {
                continue;
            }

            if(lazyDecoratedServices == null) {
                lazyDecoratedServices = new LazyDecoratedServiceValues(psiElement.getProject());
            }

            // services -> service_name
            visitServiceId(psiElement, (YAMLKeyValue) yamlKeyValue, result, lazyDecoratedServices);
        }
    }

    private void visitServiceId(@NotNull PsiElement leafTarget, @NotNull YAMLKeyValue yamlKeyValue, @NotNull Collection<LineMarkerInfo> result, @NotNull LazyDecoratedServiceValues lazyDecoratedServices) {
        String id = yamlKeyValue.getKeyText();
        if(StringUtils.isBlank(id)) {
            return;
        }

        // decorates: @foobar
        String decorates = YamlHelper.getYamlKeyValueAsString(yamlKeyValue, "decorates");
        if(decorates != null && StringUtils.isNotBlank(decorates)) {
            result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(leafTarget, decorates));
        }

        NavigationGutterIconBuilder<PsiElement> lineMarker = ServiceUtil.getLineMarkerForDecoratedServiceId(
            yamlKeyValue.getProject(),
            lazyDecoratedServices.getDecoratedServices(),
            id
        );

        if(lineMarker == null) {
            return;
        }

        result.add(lineMarker.createLineMarkerInfo(leafTarget));
    }
}
