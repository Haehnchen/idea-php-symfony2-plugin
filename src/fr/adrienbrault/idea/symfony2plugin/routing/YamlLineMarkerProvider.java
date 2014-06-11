package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLHash;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Collection;
import java.util.List;

public class YamlLineMarkerProvider implements LineMarkerProvider {

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> psiElements, @NotNull Collection<LineMarkerInfo> lineMarkerInfos) {

        for(PsiElement psiElement : psiElements) {
            attachRouteActions(lineMarkerInfos, psiElement);
        }

    }

    private void attachRouteActions(Collection<LineMarkerInfo> lineMarkerInfos, PsiElement psiElement) {

        /*
         * foo:
         *   defaults: { _controller: "Bundle:Foo:Bar" }
         */
        if(psiElement instanceof YAMLKeyValue && psiElement.getParent() instanceof YAMLDocument) {
            YAMLKeyValue yamlKeyValue = YamlHelper.getYamlKeyValue((YAMLKeyValue) psiElement, "defaults");
            if(yamlKeyValue != null) {
                YAMLCompoundValue yamlCompoundValue = PsiTreeUtil.getChildOfType(yamlKeyValue, YAMLCompoundValue.class);
                if(yamlCompoundValue != null) {

                    // @TODO: not only support hash elements
                    YAMLHash yamlHashElement = PsiTreeUtil.getChildOfType(yamlCompoundValue, YAMLHash.class);
                    if(yamlHashElement != null) {
                        YAMLKeyValue yamlKeyValue1 = YamlHelper.getYamlKeyValue(yamlHashElement, "_controller", true);
                        if(yamlKeyValue1 != null) {

                            Method method = ControllerIndex.getControllerMethod(psiElement.getProject(), yamlKeyValue1.getValueText());
                            if(method != null) {
                                NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.METHOD).
                                    setTargets(method).
                                    setTooltipText("Navigate to action");

                                lineMarkerInfos.add(builder.createLineMarkerInfo(psiElement));
                            }

                        }

                    }
                }

            }

        }
    }

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

}
