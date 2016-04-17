package fr.adrienbrault.idea.symfony2plugin.dic.container.instance;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlServiceContainerAnnotator;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlConstructServiceSuggestionCollector implements ServiceSuggestionCollector {

    @NotNull
    public Collection<String> collect(@NotNull PsiElement psiElement, @NotNull Collection<ContainerService> serviceMap) {
        if(!(psiElement.getContainingFile() instanceof XmlFile) || psiElement.getNode().getElementType() != XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
            return Collections.emptyList();
        }

        XmlAttributeValue xmlAttributeValue = PsiTreeUtil.getParentOfType(psiElement, XmlAttributeValue.class);
        if(xmlAttributeValue == null) {
            return Collections.emptyList();
        }

        XmlTag argumentTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
        if(argumentTag == null) {
            return Collections.emptyList();
        }

        XmlTag serviceTag = PsiElementAssertUtil.getParentOfTypeOrNull(argumentTag, XmlTag.class);
        if(serviceTag == null) {
            return Collections.emptyList();
        }

        if(!serviceTag.getName().equals("service")) {
            return Collections.emptyList();
        }

        // service/argument[id]

        String serviceDefName = serviceTag.getAttributeValue("class");
        if(serviceDefName != null) {
            PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), serviceDefName);

            // check type hint on constructor
            if(phpClass != null) {
                Method constructor = phpClass.getConstructor();
                if(constructor != null) {

                    Collection<ContainerService> suggestions = ServiceUtil.getServiceSuggestionsForTypeHint(
                        constructor,
                        XmlServiceContainerAnnotator.getArgumentIndex(argumentTag),
                        serviceMap
                    );

                    if(suggestions.size() == 0) {
                        return Collections.emptyList();
                    }

                    return ContainerUtil.map(suggestions, new Function<ContainerService, String>() {
                        @Override
                        public String fun(ContainerService containerService) {
                            return containerService.getName();
                        }
                    });
                }
            }

        }

        return Collections.emptyList();
    }

}
