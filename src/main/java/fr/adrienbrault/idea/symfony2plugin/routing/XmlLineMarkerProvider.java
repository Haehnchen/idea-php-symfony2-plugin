package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.patterns.XmlPatterns;
import com.intellij.patterns.XmlTagPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlLineMarkerProvider implements LineMarkerProvider {

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> psiElements, @NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos) {
        if(psiElements.isEmpty() || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        PsiFile containingFile = psiElements.get(0).getContainingFile();
        if (containingFile == null || !XmlHelper.isXmlFileExtension(containingFile)) {
            return;
        }

        var xmlTagNameLeafPattern = XmlHelper.getXmlTagNameLeafStartPattern();
        var routeTagPattern = Pattern.getRouteTag();
        var routeImportPattern = Pattern.getRouteImport();

        for(PsiElement psiElement: psiElements) {
            if(xmlTagNameLeafPattern.accepts(psiElement)) {
                attachRouteActions(psiElement, lineMarkerInfos, routeTagPattern);
                attachRouteImport(psiElement, lineMarkerInfos, routeImportPattern);



            } else if(psiElement instanceof XmlFile) {
                RelatedItemLineMarkerInfo<PsiElement> lineMarker = FileResourceUtil.getFileImplementsLineMarker((PsiFile) psiElement);
                if(lineMarker != null) {
                    lineMarkerInfos.add(lineMarker);
                }
            }
        }

    }

    private void attachRouteActions(@NotNull PsiElement psiElement, @NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos, @NotNull XmlTagPattern.Capture routeTagPattern) {
        PsiElement xmlTag = psiElement.getParent();
        if(!(xmlTag instanceof XmlTag) || !routeTagPattern.accepts(xmlTag)) {
            return;
        }

        String controller = RouteHelper.getXmlController((XmlTag) xmlTag);
        if (controller != null) {
            PsiElement[] methods = RouteHelper.getMethodsOnControllerShortcut(xmlTag.getProject(), controller);
            if (methods.length > 0) {
                RouteHelper.addControllerLineMarker(lineMarkerInfos, methods, psiElement);
            }
        }
    }

    private void attachRouteImport(@NotNull PsiElement psiElement, @NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos, @NotNull XmlTagPattern.Capture routeImportPattern) {
        PsiElement xmlTag = psiElement.getParent();
        if(!(xmlTag instanceof XmlTag) || !routeImportPattern.accepts(xmlTag)) {
            return;
        }

        String type = ((XmlTag) xmlTag).getAttributeValue("type");
        if (!"annotation".equals(type) && !"attribute".equals(type)) {
            return;
        }

        String resource = ((XmlTag) xmlTag).getAttributeValue("resource");
        if (StringUtils.isBlank(resource)) {
            return;
        }

        lineMarkerInfos.add(
            FileResourceUtil.getNavigationGutterForRouteAnnotationResources(
                psiElement.getProject(),
                psiElement.getContainingFile().getVirtualFile(),
                resource
            ).createLineMarkerInfo(psiElement)
        );
    }

    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    private static class Pattern {
        // Matches <route> elements directly inside a <routes> container in XML routing files
        static XmlTagPattern.Capture getRouteTag() {
            return XmlPatterns.xmlTag().withName("route").withParent(
                XmlPatterns.xmlTag().withName("routes")
            ).inFile(XmlHelper.getXmlFilePattern());
        }

        // Matches <import> elements directly inside a <routes> container in XML routing files
        static XmlTagPattern.Capture getRouteImport() {
            return XmlPatterns.xmlTag().withName("import").withParent(
                XmlPatterns.xmlTag().withName("routes")
            ).inFile(XmlHelper.getXmlFilePattern());
        }
    }
}

