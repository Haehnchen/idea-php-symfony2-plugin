package fr.adrienbrault.idea.symfonyplugin.routing;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.patterns.XmlPatterns;
import com.intellij.patterns.XmlTagPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfonyplugin.Symfony2Icons;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfonyplugin.util.resource.FileResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlLineMarkerProvider implements LineMarkerProvider {

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> psiElements, @NotNull Collection<LineMarkerInfo> lineMarkerInfos) {
        if(psiElements.size() == 0 || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        for(PsiElement psiElement: psiElements) {
            if(XmlHelper.getXmlTagNameLeafStartPattern().accepts(psiElement)) {
                attachRouteActions(psiElement, lineMarkerInfos);
            } else if(psiElement instanceof XmlFile) {
                RelatedItemLineMarkerInfo<PsiElement> lineMarker = FileResourceUtil.getFileImplementsLineMarker((PsiFile) psiElement);
                if(lineMarker != null) {
                    lineMarkerInfos.add(lineMarker);
                }
            }
        }

    }

    private void attachRouteActions(@NotNull PsiElement psiElement, @NotNull Collection<LineMarkerInfo> lineMarkerInfos) {
        PsiElement xmlTag = psiElement.getParent();
        if(!(xmlTag instanceof XmlTag) || !Pattern.getRouteTag().accepts(xmlTag)) {
            return;
        }

        String controller = RouteHelper.getXmlController((XmlTag) xmlTag);
        if(controller != null) {
            PsiElement[] methods = RouteHelper.getMethodsOnControllerShortcut(xmlTag.getProject(), controller);
            if(methods.length > 0) {
                NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.TWIG_CONTROLLER_LINE_MARKER).
                    setTargets(methods).
                    setTooltipText("Navigate to action");

                lineMarkerInfos.add(builder.createLineMarkerInfo(psiElement));
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
