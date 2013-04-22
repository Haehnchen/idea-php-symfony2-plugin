package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.patterns.*;
import com.intellij.psi.*;
import fr.adrienbrault.idea.symfony2plugin.config.xml.provider.ServiceReferenceProvider;

public class XmlReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar registrar) {

        // <argument type="service" id="service_container" />
        registrar.registerReferenceProvider(
                XmlPatterns.xmlAttributeValue().withParent(
                        XmlPatterns.xmlAttribute("id").withParent(
                                XmlPatterns.xmlTag().withChild(
                                        XmlPatterns.xmlAttribute("type").withValue(
                                                StandardPatterns.string().equalTo("service")
                                        )
                                )
                        )
        ), new ServiceReferenceProvider());

    }

}