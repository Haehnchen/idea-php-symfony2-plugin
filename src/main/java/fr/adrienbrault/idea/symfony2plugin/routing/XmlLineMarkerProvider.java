package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.patterns.XmlPatterns;
import com.intellij.patterns.XmlTagPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.ServiceLineMarkerProvider;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlLineMarkerProvider implements LineMarkerProvider {

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> psiElements, @NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos) {
        if(psiElements.size() == 0 || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        PsiFile containingFile = psiElements.get(0).getContainingFile();
        if (containingFile == null || !XmlHelper.isXmlFileExtension(containingFile)) {
            return;
        }

        for(PsiElement psiElement: psiElements) {
            if(XmlHelper.getXmlTagNameLeafStartPattern().accepts(psiElement)) {
                attachRouteActions(psiElement, lineMarkerInfos);
                attachRouteImport(psiElement, lineMarkerInfos);



            } else if(psiElement instanceof XmlFile) {
                RelatedItemLineMarkerInfo<PsiElement> lineMarker = FileResourceUtil.getFileImplementsLineMarker((PsiFile) psiElement);
                if(lineMarker != null) {
                    lineMarkerInfos.add(lineMarker);
                }
            }
        }

    }

    private void attachRouteActions(@NotNull PsiElement psiElement, @NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos) {
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

    private void attachRouteImport(@NotNull PsiElement psiElement, @NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos) {
        PsiElement xmlTag = psiElement.getParent();
        if(!(xmlTag instanceof XmlTag) || !Pattern.getRouteImport().accepts(xmlTag)) {
            return;
        }

        if (!"annotation".equals(((XmlTag) xmlTag).getAttributeValue("type"))) {
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

        public static XmlTagPattern.Capture getRouteTag() {
            return XmlPatterns.xmlTag().withName("route").withParent(
                XmlPatterns.xmlTag().withName("routes")
            ).inFile(XmlHelper.getXmlFilePattern());
        }

        public static XmlTagPattern.Capture getRouteImport() {
            return XmlPatterns.xmlTag().withName("import").withParent(
                XmlPatterns.xmlTag().withName("routes")
            ).inFile(XmlHelper.getXmlFilePattern());
        }
    }

}
