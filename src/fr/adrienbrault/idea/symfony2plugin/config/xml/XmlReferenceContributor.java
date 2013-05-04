package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.patterns.*;
import com.intellij.psi.*;
import fr.adrienbrault.idea.symfony2plugin.config.xml.provider.ClassReferenceProvider;
import fr.adrienbrault.idea.symfony2plugin.config.xml.provider.ServiceReferenceProvider;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar registrar) {

        // <argument type="service" id="service_container" />
        registrar.registerReferenceProvider(
            XmlPatterns
                .xmlAttributeValue()
                .withParent(XmlPatterns
                    .xmlAttribute("id")
                    .withParent(XmlPatterns
                        .xmlTag()
                        .withChild(XmlPatterns
                            .xmlAttribute("type")
                            .withValue(
                                StandardPatterns.string().equalTo("service")
                            )
                        )
                    )
                ),

            new ServiceReferenceProvider()
        );

        // <service id="fos_user.user_provider.username" class="FOS\UserBundle\Security\UserProvider">
        registrar.registerReferenceProvider(
            XmlPatterns
                .xmlAttributeValue()
                .withParent(XmlPatterns
                    .xmlAttribute("class")
                    .withValue(StandardPatterns
                        .string().contains("\\")
                    )
                    .withParent(XmlPatterns
                        .xmlTag()
                        .withChild(
                            XmlPatterns.xmlAttribute("id")
                        )
                    )
                ),

            new ClassReferenceProvider()
        );

    }

}