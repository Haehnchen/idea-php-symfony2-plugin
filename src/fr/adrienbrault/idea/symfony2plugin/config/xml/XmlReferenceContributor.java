package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.*;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.ClassPublicMethodReference;
import fr.adrienbrault.idea.symfony2plugin.config.PhpClassReference;
import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterReference;
import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterReferenceProvider;
import fr.adrienbrault.idea.symfony2plugin.config.dic.EventDispatcherEventReference;
import fr.adrienbrault.idea.symfony2plugin.config.xml.provider.ClassReferenceProvider;
import fr.adrienbrault.idea.symfony2plugin.config.xml.provider.ServiceReferenceProvider;
import fr.adrienbrault.idea.symfony2plugin.dic.TagReference;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar registrar) {

        // <argument type="service" id="service_container" />
        registrar.registerReferenceProvider(
            XmlHelper.getArgumentServiceIdPattern(),
            new ServiceReferenceProvider()
        );

        // <service class="%foo.class%">
        // <service class="Class\Name">
        registrar.registerReferenceProvider(
            XmlHelper.getServiceIdPattern(),

            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    if(!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement instanceof XmlAttributeValue)) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new ServiceIdReference(
                        (XmlAttributeValue) psiElement)
                    };

                }
            }
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
                        ).inside(
                            XmlHelper.getInsideTagPattern("services")
                    ).inFile(XmlHelper.getXmlFilePattern())
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
                    ).inside(
                        XmlHelper.getInsideTagPattern("services")
                    ).inFile(XmlHelper.getXmlFilePattern())
            ),

            new ClassReferenceProvider(false)
        );

        // <service id="fos_user.group_manager.default" class="%fos_user.group_manager.class%"
        // <argument>%form.resolved_type_factory.class%</argument>
        registrar.registerReferenceProvider(

            XmlPatterns
                .psiElement(XmlTokenType.XML_DATA_CHARACTERS)
                .withText(StandardPatterns.string().startsWith("%"))
                .withParent(XmlPatterns
                    .xmlText()
                    .withParent(XmlPatterns
                        .xmlTag()
                        .withName("argument")
                    )
                ).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern()),


            new ParameterReferenceProvider().setTrimPercent(true).setTrimQuote(true)
        );

        // <tag name="kernel.event_subscriber" />
        registrar.registerReferenceProvider(
            XmlHelper.getTagAttributePattern("tag", "name")
                .inside(XmlHelper.getInsideTagPattern("services"))
            .inFile(XmlHelper.getXmlFilePattern()),

            new PsiReferenceProvider() {

                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {

                    if(!Symfony2ProjectComponent.isEnabled(element)) {
                        return new PsiReference[0];
                    }

                    if(element instanceof XmlAttributeValue) {
                        return new PsiReference[] { new TagReference(element, PsiElementUtils.trimQuote(element.getText()))};
                    }

                    return new PsiReference[0];
                }
            }
        );


        // <tag event="foo" method="kernel.event_subscriber" />
        registrar.registerReferenceProvider(
            XmlHelper.getTagAttributePattern("tag", "method")
                .inside(XmlHelper.getInsideTagPattern("services"))
                .inFile(XmlHelper.getXmlFilePattern()),
            new ClassMethodReferenceProvider()
        );

        registrar.registerReferenceProvider(
            XmlHelper.getTagAttributePattern("call", "method")
                .inside(XmlHelper.getInsideTagPattern("services"))
                .inFile(XmlHelper.getXmlFilePattern()),
            new ClassMethodReferenceProvider()
        );

        registrar.registerReferenceProvider(
            XmlPatterns.or(
                XmlHelper.getParameterWithClassEndingPattern()
                    .inside(XmlHelper.getInsideTagPattern("parameters"))
                    .inFile(XmlHelper.getXmlFilePattern()
                )
            ),
            new PsiReferenceProvider() {

                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {

                    if(!Symfony2ProjectComponent.isEnabled(element)) {
                        return new PsiReference[0];
                    }

                    if(element instanceof XmlToken) {
                        return new PsiReference[] {
                            new PhpClassReference(element, PsiElementUtils.removeIdeaRuleHack(PsiElementUtils.trimQuote(element.getText())), true)
                        };
                    }

                    return new PsiReference[0];
                }
            }
        );

        registrar.registerReferenceProvider(
            XmlHelper.getTagAttributePattern("tag", "event").inside(XmlHelper.getInsideTagPattern("services")),
            new PsiReferenceProvider() {

                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {

                    if(!Symfony2ProjectComponent.isEnabled(element)) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[] {
                        new EventDispatcherEventReference(element, PsiElementUtils.removeIdeaRuleHack(PsiElementUtils.trimQuote(element.getText())))
                    };

                }
            }
        );

    }

    private class ClassMethodReferenceProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext context) {

            if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return new PsiReference[0];
            }

            // check for valid xml file and services container
            if(!XmlPatterns.psiElement().inside(XmlHelper.getInsideTagPattern("services")).inFile(XmlHelper.getXmlFilePattern()).accepts(psiElement)) {
                return new PsiReference[0];
            }

            // search for parent service definition
            XmlTag callXmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
            XmlTag xmlTag = PsiTreeUtil.getParentOfType(callXmlTag, XmlTag.class);
            if(xmlTag == null || !xmlTag.getName().equals("service")) {
                return new PsiReference[0];
            }

            XmlAttribute classAttribute = xmlTag.getAttribute("class");
            if(classAttribute == null) {
                return new PsiReference[0];
            }

            return new PsiReference[] { new ClassPublicMethodReference(psiElement, classAttribute.getValue())};
        }
    }

    private static class ServiceIdReference extends PsiPolyVariantReferenceBase<PsiElement> {

        private final XmlAttributeValue psiElement;

        public ServiceIdReference(XmlAttributeValue psiElement) {
            super(psiElement);
            this.psiElement = psiElement;
        }

        @NotNull
        @Override
        public ResolveResult[] multiResolve(boolean b) {
            String value = this.psiElement.getValue();
            return PsiElementResolveResult.createResults(ServiceUtil.getServiceClassTargets(getElement().getProject(), value));
        }

        @NotNull
        @Override
        public Object[] getVariants() {
            return new Object[0];
        }

    }

}