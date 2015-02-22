package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.*;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.ClassPublicMethodReference;
import fr.adrienbrault.idea.symfony2plugin.config.PhpClassReference;
import fr.adrienbrault.idea.symfony2plugin.config.dic.EventDispatcherEventReference;
import fr.adrienbrault.idea.symfony2plugin.config.xml.provider.ServiceReferenceProvider;
import fr.adrienbrault.idea.symfony2plugin.dic.TagReference;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

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

        // <factory service="factory_service" />
        registrar.registerReferenceProvider(
            XmlHelper.getFactoryServiceCompletionPattern(),
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
        registrar.registerReferenceProvider(
            XmlHelper.getParameterClassValuePattern(),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    // get the service name "service_container"
                    String text = psiElement.getText();
                    return new PsiReference[]{ new PhpClassReference(psiElement, text) };

                }
            }
        );

        // <argument>%form.resolved_type_factory.class%</argument>
        registrar.registerReferenceProvider(
            XmlHelper.getArgumentValuePattern(),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    PsiElement parent = psiElement.getParent();
                    if(!(parent instanceof XmlText)) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new ParameterXmlReference(((XmlText) parent)) };
                }
            }
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
            XmlHelper.getTagAttributePattern("factory", "method")
                .inside(XmlHelper.getInsideTagPattern("services"))
                .inFile(XmlHelper.getXmlFilePattern()),
            new FactoryClassMethodReferenceProvider()
        );

        registrar.registerReferenceProvider(

            XmlHelper.getParameterWithClassEndingPattern()
                .inside(XmlHelper.getInsideTagPattern("parameters"))
                .inFile(XmlHelper.getXmlFilePattern()

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

    private class FactoryClassMethodReferenceProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext context) {

            if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return new PsiReference[0];
            }

            XmlTag callXmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
            if(callXmlTag == null) {
                return new PsiReference[0];
            }

            XmlAttribute aClass = callXmlTag.getAttribute("service");
            if(aClass == null) {
                return new PsiReference[0];
            }

            String value = aClass.getValue();
            if(StringUtils.isBlank(value)) {
                return new PsiReference[0];
            }

            return new PsiReference[] { new ClassPublicMethodReference(psiElement, value)};
        }
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

            String serviceDefinitionClass = XmlHelper.getServiceDefinitionClass(psiElement);
            if(serviceDefinitionClass == null) {
                return new PsiReference[0];
            }

            return new PsiReference[] { new ClassPublicMethodReference(psiElement, serviceDefinitionClass)};
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


            Collection<PsiElement> serviceClassTargets = ServiceUtil.getServiceClassTargets(getElement().getProject(), value);

            // @TODO: on implement multiple service resolve; we can make it nicer here
            // self add on compiler parameter, in this case we dont have a target;
            // to not get ide warnings
            if(serviceClassTargets.size() == 0 && value.startsWith("%") && value.endsWith("%") && ContainerCollectionResolver.getParameterNames(getElement().getProject()).contains(value.substring(1, value.length() - 1))) {
                serviceClassTargets.add(getElement());
            }

            return PsiElementResolveResult.createResults(serviceClassTargets);
        }

        @NotNull
        @Override
        public Object[] getVariants() {
            return new Object[0];
        }

    }

}