package fr.adrienbrault.idea.symfony2plugin.dic.linemarker;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.patterns.XmlPatterns;
import com.intellij.patterns.XmlTagPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ContainerBuilderCall;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerBuilderStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    private Map<String, Collection<ContainerService>> decoratedServiceCache;

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

        for (PsiElement psiElement : psiElements) {
            // <services><service id="foo"/></services>
            if(psiElement instanceof XmlTag && getServiceIdPattern().accepts(psiElement)) {
                visitServiceId((XmlTag) psiElement, result);
            }
        }

        decoratedServiceCache = null;
    }

    /**
     * <service id="foo"/>
     */
    private void visitServiceId(@NotNull XmlTag xmlTag, @NotNull Collection<LineMarkerInfo> result) {
        String id = xmlTag.getAttributeValue("id");
        if(StringUtils.isBlank(id)) {
            return;
        }

        visitDecorates(xmlTag, result, id);
        visitDefinition(xmlTag, result, id);
    }

    private void visitDefinition(@NotNull XmlTag xmlTag, @NotNull Collection<LineMarkerInfo> result, @NotNull String id) {
        for (ContainerBuilderCall call : FileBasedIndex.getInstance().getValues(ContainerBuilderStubIndex.KEY, "getDefinition", GlobalSearchScope.allScope(xmlTag.getProject()))) {
            if(parameter != null) {
                Collection<String> parameter = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

                Collection<String> serviceParameter = call.getParameter();
                if(serviceParameter != null) {
                    parameter.addAll(serviceParameter);

                    if(serviceParameter.contains(id)) {
                        result.add(ServiceUtil.getLineMarkerForDefinitionServiceId());
                    }
                }
            }
        }
    }

    private void visitDecorates(@NotNull XmlTag xmlTag, @NotNull Collection<LineMarkerInfo> result, @NotNull String id) {
        // <service id="foo" decorates=foobar" />
        String decorates = xmlTag.getAttributeValue("decorates");
        if(StringUtils.isNotBlank(decorates)) {
            result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(xmlTag, decorates, result));
        }

        if(this.decoratedServiceCache == null) {
            this.decoratedServiceCache = ServiceIndexUtil.getDecoratedServices(xmlTag.getProject());
        }

        NavigationGutterIconBuilder<PsiElement> lineMarker = ServiceUtil.getLineMarkerForDecoratedServiceId(
            xmlTag.getProject(), this.decoratedServiceCache, id
        );

        if(lineMarker == null) {
            return;
        }

        result.add(lineMarker.createLineMarkerInfo(xmlTag));
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
