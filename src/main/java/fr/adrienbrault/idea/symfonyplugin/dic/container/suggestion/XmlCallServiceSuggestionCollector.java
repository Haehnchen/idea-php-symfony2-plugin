package fr.adrienbrault.idea.symfonyplugin.dic.container.suggestion;

import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTokenType;
import fr.adrienbrault.idea.symfonyplugin.dic.ContainerService;
import fr.adrienbrault.idea.symfonyplugin.dic.container.suggestion.utils.ServiceSuggestionUtil;
import fr.adrienbrault.idea.symfonyplugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfonyplugin.stubs.ContainerCollectionResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * <service class="Foo">
 *   <call method="setFoo">
 *     <argument type="service" id="<caret>" />
 *   </call>
 * </service>
 */
public class XmlCallServiceSuggestionCollector implements ServiceSuggestionCollector {

    @NotNull
    public Collection<String> collect(@NotNull PsiElement psiElement, @NotNull Collection<ContainerService> serviceMap) {
        if(!(psiElement.getContainingFile() instanceof XmlFile) || psiElement.getNode().getElementType() != XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
            return Collections.emptyList();
        }

        return ServiceSuggestionUtil.createSuggestions(ServiceContainerUtil.getXmlCallTypeHint(
            psiElement, new ContainerCollectionResolver.LazyServiceCollector(psiElement.getProject())
        ), serviceMap);
    }
}
