package fr.adrienbrault.idea.symfony2plugin.dic.linemarker;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.patterns.XmlPatterns;
import com.intellij.patterns.XmlTagPattern;
import com.intellij.psi.PsiElement;
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
        if(psiElements.size() == 0 || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        LazyDecoratedParentServiceValues lazyDecoratedParentServiceValues = null;

        for (PsiElement psiElement : psiElements) {
            if(!XmlHelper.getXmlTagNameLeafStartPattern().accepts(psiElement)) {
                continue;
            }

            PsiElement xmlTag = psiElement.getParent();
            if(!(xmlTag instanceof XmlTag) || !getServiceIdPattern().accepts(xmlTag)) {
                continue;
            }

            if(lazyDecoratedParentServiceValues == null) {
                lazyDecoratedParentServiceValues = new LazyDecoratedParentServiceValues(psiElement.getProject());
            }

            // <services><service id="foo"/></services>
            visitServiceId(psiElement, (XmlTag) xmlTag, result, lazyDecoratedParentServiceValues);
        }
    }

    /**
     * <service id="foo"/>
     */
    private void visitServiceId(@NotNull PsiElement leafTarget, @NotNull XmlTag xmlTag, @NotNull Collection<? super LineMarkerInfo<?>> result, @NotNull LazyDecoratedParentServiceValues lazyDecoratedParentServiceValues) {
        String id = xmlTag.getAttributeValue("id");
        if(StringUtils.isBlank(id)) {
            return;
        }

        // <service decorates="foobar" />
        String decorates = xmlTag.getAttributeValue("decorates");
        if(decorates != null && StringUtils.isNotBlank(decorates)) {
            result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(leafTarget, ServiceUtil.ServiceLineMarker.DECORATE, decorates));
        }

        // <service parent="foobar" />
        String parent = xmlTag.getAttributeValue("parent");
        if(parent != null && StringUtils.isNotBlank(parent)) {
            result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(leafTarget, ServiceUtil.ServiceLineMarker.PARENT, parent));
        }

        // foreign "decorates" linemarker
        NavigationGutterIconBuilder<PsiElement> lineMarkerDecorates = ServiceUtil.getLineMarkerForDecoratedServiceId(
            xmlTag.getProject(),
            ServiceUtil.ServiceLineMarker.DECORATE,
            lazyDecoratedParentServiceValues.getDecoratedServices(),
            id
        );

        if(lineMarkerDecorates != null) {
            result.add(lineMarkerDecorates.createLineMarkerInfo(leafTarget));
        }

        // foreign "parent" linemarker
        NavigationGutterIconBuilder<PsiElement> lineMarkerParent = ServiceUtil.getLineMarkerForDecoratedServiceId(
            xmlTag.getProject(),
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
}
