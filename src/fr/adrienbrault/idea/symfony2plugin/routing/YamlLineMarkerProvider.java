package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
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

        if(psiElements.size() == 0 || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        for(PsiElement psiElement : psiElements) {
            attachRouteActions(lineMarkerInfos, psiElement);
            attachEntityClass(lineMarkerInfos, psiElement);
        }

    }

    private void attachEntityClass(Collection<LineMarkerInfo> lineMarkerInfos, PsiElement psiElement) {

        if(psiElement instanceof YAMLKeyValue && psiElement.getParent() instanceof YAMLDocument) {

            PsiFile containingFile;
            try {
                containingFile = psiElement.getContainingFile();
            } catch (PsiInvalidElementAccessException e) {
                return;
            }

            String fileName = containingFile.getName();
            if(fileName.endsWith("orm.yml") || fileName.endsWith("odm.yml") || fileName.endsWith("mongodb.yml")) {
                String keyText = ((YAMLKeyValue) psiElement).getKeyText();
                if(StringUtils.isNotBlank(keyText)) {
                    PhpClass phpClass = PhpElementsUtil.getClass(psiElement.getProject(), keyText);
                    if(phpClass != null) {
                        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.DOCTRINE_LINE_MARKER).
                            setTargets(phpClass).
                            setTooltipText("Navigate to class");

                        lineMarkerInfos.add(builder.createLineMarkerInfo(psiElement));
                    }
                }
            }
        }
    }

    private void attachRouteActions(Collection<LineMarkerInfo> lineMarkerInfos, PsiElement psiElement) {

        /*
         * foo:
         *   defaults: { _controller: "Bundle:Foo:Bar" }
         *   defaults:
         *      _controller: "Bundle:Foo:Bar"
         */
        if(psiElement instanceof YAMLKeyValue && psiElement.getParent() instanceof YAMLDocument) {
            YAMLKeyValue yamlKeyValue = YamlHelper.getYamlKeyValue((YAMLKeyValue) psiElement, "defaults");
            if(yamlKeyValue != null) {
                YAMLCompoundValue yamlCompoundValue = PsiTreeUtil.getChildOfType(yamlKeyValue, YAMLCompoundValue.class);
                if(yamlCompoundValue != null) {

                    // if we have a child of YAMLKeyValue, we need to go back to parent
                    // else on YAMLHash we can directly visit array keys
                    PsiElement yamlHashElement = PsiTreeUtil.getChildOfAnyType(yamlCompoundValue, YAMLHash.class, YAMLKeyValue.class);
                    if(yamlHashElement instanceof YAMLKeyValue) {
                        yamlHashElement = yamlCompoundValue;
                    }

                    if(yamlHashElement != null) {
                        YAMLKeyValue yamlKeyValueController = YamlHelper.getYamlKeyValue(yamlHashElement, "_controller", true);
                        if(yamlKeyValueController != null) {

                            Method method = ControllerIndex.getControllerMethod(psiElement.getProject(), yamlKeyValueController.getValueText());
                            if(method != null) {
                                NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.TWIG_CONTROLLER_LINE_MARKER).
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
