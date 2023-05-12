package fr.adrienbrault.idea.symfony2plugin.dic.linemarker;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.patterns.XmlPatterns;
import com.intellij.patterns.XmlTagPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlLineMarkerProvider implements LineMarkerProvider {
    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> psiElements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        if (psiElements.size() == 0) {
            return;
        }

        Project project = psiElements.get(0).getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        PsiFile containingFile = psiElements.get(0).getContainingFile();
        if (containingFile == null || !XmlHelper.isXmlFileExtension(containingFile)) {
            return;
        }

        LazyDecoratedParentServiceValues lazyDecoratedParentServiceValues = null;

        for (PsiElement psiElement : psiElements) {
            if(!XmlHelper.getXmlTagNameLeafStartPattern().accepts(psiElement)) {
                continue;
            }

            PsiElement xmlTag = psiElement.getParent();
            if(!(xmlTag instanceof XmlTag)) {
                continue;
            }

            if (getServiceIdPattern().accepts(xmlTag)) {
                if(lazyDecoratedParentServiceValues == null) {
                    lazyDecoratedParentServiceValues = new LazyDecoratedParentServiceValues(project);
                }

                // <services><service id="foo"/></services>
                visitServiceId(project, psiElement, (XmlTag) xmlTag, result, lazyDecoratedParentServiceValues);

                continue;
            }

            if (getPrototypeNamespacePattern().accepts(xmlTag)) {
                String namespace = ((XmlTag) xmlTag).getAttributeValue("namespace");
                if (StringUtils.isBlank(namespace)) {
                    continue;
                }

                String resource = ((XmlTag) xmlTag).getAttributeValue("resource");
                if (StringUtils.isBlank(resource)) {
                    continue;
                }

                result.add(NavigationGutterIconBuilder.create(AllIcons.Modules.SourceRoot)
                    .setTargets(NotNullLazyValue.lazy(() -> XmlHelper.getNamespaceResourcesClasses((XmlTag) xmlTag)))
                    .setTooltipText("Navigate to class")
                    .createLineMarkerInfo(psiElement));
            }
        }
    }

    /**
     * <service id="foo"/>
     */
    private void visitServiceId(@NotNull Project project, @NotNull PsiElement leafTarget, @NotNull XmlTag xmlTag, @NotNull Collection<? super LineMarkerInfo<?>> result, @NotNull LazyDecoratedParentServiceValues lazyDecoratedParentServiceValues) {
        String id = xmlTag.getAttributeValue("id");
        if(StringUtils.isBlank(id)) {
            return;
        }

        // <service decorates="foobar" />
        String decorates = xmlTag.getAttributeValue("decorates");
        if(StringUtils.isNotBlank(decorates)) {
            result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(leafTarget, ServiceUtil.ServiceLineMarker.DECORATE, decorates));
        }

        // <service parent="foobar" />
        String parent = xmlTag.getAttributeValue("parent");
        if(StringUtils.isNotBlank(parent)) {
            result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(leafTarget, ServiceUtil.ServiceLineMarker.PARENT, parent));
        }

        // foreign "decorates" linemarker
        NavigationGutterIconBuilder<PsiElement> lineMarkerDecorates = ServiceUtil.getLineMarkerForDecoratedServiceId(
            project,
            ServiceUtil.ServiceLineMarker.DECORATE,
            lazyDecoratedParentServiceValues.getDecoratedServices(),
            id
        );

        if(lineMarkerDecorates != null) {
            result.add(lineMarkerDecorates.createLineMarkerInfo(leafTarget));
        }

        // foreign "parent" linemarker
        NavigationGutterIconBuilder<PsiElement> lineMarkerParent = ServiceUtil.getLineMarkerForDecoratedServiceId(
            project,
            ServiceUtil.ServiceLineMarker.PARENT,
            lazyDecoratedParentServiceValues.getParentServices(),
            id
        );

        if(lineMarkerParent != null) {
            result.add(lineMarkerParent.createLineMarkerInfo(leafTarget));
        }
    }

    /**
     * <service id="%foo.class%"/>
     */
    private static XmlTagPattern.Capture getServiceIdPattern() {
        return XmlPatterns.xmlTag().withName("service")
            .withChild(XmlPatterns.xmlAttribute().withName("id")).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern());
    }

    /**
     * <prototype namespace="App\" resource="../src/*" exclude="../src/{DependencyInjection,Entity,Tests,Kernel.php}"/>
     */
    private static XmlTagPattern.Capture getPrototypeNamespacePattern() {
        return XmlPatterns.xmlTag().withName("prototype")
            .withChild(XmlPatterns.xmlAttribute().withName("namespace")).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern());
    }
}
