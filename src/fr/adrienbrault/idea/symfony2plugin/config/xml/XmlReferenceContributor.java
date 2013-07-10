package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.patterns.*;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlTokenType;
import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterReferenceProvider;
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
                ).inFile(getXmlFilePattern()),

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
                ).inFile(getXmlFilePattern()),

            new ClassReferenceProvider()
        );

        // <parameter key="fos_user.user_manager.class">FOS\UserBundle\Doctrine\UserManager</parameter>
        // <argument>FOS\UserBundle\Doctrine\UserManager</argument>
        registrar.registerReferenceProvider(
            XmlPatterns.or(
                XmlPatterns
                    .psiElement(XmlTokenType.XML_DATA_CHARACTERS)
                    .withText(StandardPatterns.string().contains("\\"))
                    .withParent(XmlPatterns
                        .xmlText()
                        .withParent(XmlPatterns
                            .xmlTag()
                            .withName("parameter")
                            .withAnyAttribute("key")
                        ).inFile(getXmlFilePattern())
                    ),
                XmlPatterns
                    .psiElement(XmlTokenType.XML_DATA_CHARACTERS)
                    .withText(StandardPatterns.string().contains("\\"))
                    .withParent(XmlPatterns
                        .xmlText()
                        .withParent(XmlPatterns
                            .xmlTag()
                            .withName("argument")
                        )
                    ).inFile(getXmlFilePattern())
            ),

            new ClassReferenceProvider(false)
        );

        // <service id="fos_user.group_manager.default" class="%fos_user.group_manager.class%"
        // <argument>%form.resolved_type_factory.class%</argument>
        registrar.registerReferenceProvider(

            XmlPatterns.or(
                XmlPatterns
                    .psiElement(XmlTokenType.XML_DATA_CHARACTERS)
                    .withText(StandardPatterns.string().startsWith("%"))
                    .withParent(XmlPatterns
                        .xmlText()
                        .withParent(XmlPatterns
                            .xmlTag()
                            .withName("argument")
                        )
                    ).inFile(getXmlFilePattern()),
                XmlPatterns
                    .xmlAttributeValue()
                    .withParent(XmlPatterns
                        .xmlAttribute("class")
                        .withValue(StandardPatterns
                            .string().startsWith("%")
                        )
                        .withParent(XmlPatterns
                            .xmlTag()
                            .withChild(
                                XmlPatterns.xmlAttribute("id")
                            )
                        )
                    ).inFile(getXmlFilePattern())
            ),

            new ParameterReferenceProvider().setTrimPercent(true).setTrimQuote(true)
        );

    }

    private PsiFilePattern.Capture<PsiFile> getXmlFilePattern() {
        return XmlPatterns.psiFile()
            .withName(XmlPatterns
                .string().endsWith(".xml")
            );
    }

}