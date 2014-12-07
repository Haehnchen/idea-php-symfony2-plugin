package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.patterns.XmlPatterns;
import com.intellij.patterns.XmlTagPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.resolve.PhpResolveResult;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class XmlLineMarkerProvider implements LineMarkerProvider {

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> psiElements, @NotNull Collection<LineMarkerInfo> lineMarkerInfos) {

        if(psiElements.size() == 0 || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        for(PsiElement psiElement: psiElements) {
            if(psiElement instanceof XmlTag) {
                attachRouteActions((XmlTag) psiElement, lineMarkerInfos);
            }
        }

    }

    private void attachRouteActions(XmlTag xmlTag, @NotNull Collection<LineMarkerInfo> lineMarkerInfos) {

        if(!Pattern.getRouteTag().accepts(xmlTag)) {
            return;
        }

        for(XmlTag subTag : xmlTag.getSubTags()) {
            if("default".equalsIgnoreCase(subTag.getName())) {
                XmlAttribute xmlAttr = subTag.getAttribute("key");
                if(xmlAttr != null && "_controller".equals(xmlAttr.getValue())) {
                    String actionName = subTag.getValue().getTrimmedText();
                    if(StringUtils.isNotBlank(actionName)) {
                        PsiElement[] methods = RouteHelper.getMethodsOnControllerShortcut(xmlTag.getProject(), actionName);
                        if(methods.length > 0) {
                            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.TWIG_CONTROLLER_LINE_MARKER).
                                setTargets(methods).
                                setTooltipText("Navigate to action");

                            lineMarkerInfos.add(builder.createLineMarkerInfo(xmlTag));
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

    private static class Pattern {

        public static XmlTagPattern.Capture getRouteTag() {
            return XmlPatterns.xmlTag().withName("route").withParent(
                XmlPatterns.xmlTag().withName("routes")
            ).inFile(XmlHelper.getXmlFilePattern());
        }

    }

}
