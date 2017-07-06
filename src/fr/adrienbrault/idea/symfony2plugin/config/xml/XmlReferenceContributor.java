package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.xml.*;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.ClassPublicMethodReference;
import fr.adrienbrault.idea.symfony2plugin.config.PhpClassReference;
import fr.adrienbrault.idea.symfony2plugin.config.dic.EventDispatcherEventReference;
import fr.adrienbrault.idea.symfony2plugin.config.xml.provider.ServiceReferenceProvider;
import fr.adrienbrault.idea.symfony2plugin.dic.TagReference;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

        // <autowiring-type>Acme\TransformerInterface</autowiring-type>
        registrar.registerReferenceProvider(
            XmlHelper.getAutowiringTypePattern(),
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

                    String value = ((XmlText) parent).getValue();
                    return new PsiReference[]{ new PhpClassReference(psiElement, value) };
                }
            }
        );

        // <service class="%foo.class%">
        // <service class="Class\Name">
        registrar.registerReferenceProvider(
            XmlHelper.getServiceIdPattern(),
            new ClassPsiReferenceProvider()
        );

        // Symfoyn 3.3 shortcut
        // <service id="Class\Name">
        registrar.registerReferenceProvider(
            XmlHelper.getAttributePattern("id"),
            new ClassAsIdPsiReferenceProvider()
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

        // <argument type="constant">Foobar\Foo</argument>
        registrar.registerReferenceProvider(
            XmlHelper.getArgumentValueWithTypePattern("constant"),
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

                    String text = parent.getText();
                    if(StringUtils.isBlank(text)) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new ConstantXmlReference(((XmlText) parent)) };
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

        // <factory class="AppBundle\Trivago\ConfigFactory"/>
        registrar.registerReferenceProvider(
            XmlHelper.getTagAttributePattern("factory", "class")
                .inFile(XmlHelper.getXmlFilePattern()),
            new ClassPsiReferenceProvider()
        );

        // <factory class="AppBundle\Trivago\ConfigFactory" method="create"/>
        // <factory service="foo" method="create"/>
        registrar.registerReferenceProvider(
            XmlHelper.getTagAttributePattern("factory", "method")
                .inside(XmlHelper.getInsideTagPattern("services"))
                .inFile(XmlHelper.getXmlFilePattern()),
            new ChainPsiReferenceProvider(
                new FactoryClassMethodPsiReferenceProvider(),
                new FactoryServiceMethodPsiReferenceProvider()
            )
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

    private static class ClassPsiReferenceProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
            if(!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement instanceof XmlAttributeValue)) {
                return new PsiReference[0];
            }

            return new PsiReference[] {
                new ServiceIdReference((XmlAttributeValue) psiElement)
            };
        }
    }

    /**
     * Shortcut for service tag without class attribute
     *
     * <service id="Foobar\Foobar"/>
     */
    private static class ClassAsIdPsiReferenceProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
            if(!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement instanceof XmlAttributeValue)) {
                return new PsiReference[0];
            }

            PsiElement parent = psiElement.getParent();
            if(!(parent instanceof XmlAttribute) && YamlHelper.isClassServiceId(parent.getText())) {
                return new PsiReference[0];
            }

            // invalidate on class attribute
            PsiElement xmlTag = parent.getParent();
            if(!(xmlTag instanceof XmlTag) || ((XmlTag) xmlTag).getAttribute("class") != null) {
                return new PsiReference[0];
            }

            return new PsiReference[] {
                new ServiceIdWithoutParameterReference((XmlAttributeValue) psiElement)
            };
        }
    }

    /**
     * <factory service="foo" method="create"/>
     */
    private class FactoryServiceMethodPsiReferenceProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext context) {
            if(!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement instanceof XmlAttributeValue)) {
                return new PsiReference[0];
            }

            String method = ((XmlAttributeValue) psiElement).getValue();
            if(StringUtils.isBlank(method)) {
                return new PsiReference[0];
            }

            PhpClass phpClass = XmlHelper.getPhpClassForServiceFactory((XmlAttributeValue) psiElement);
            if(phpClass == null) {
                return new PsiReference[0];
            }

            Method targetMethod = phpClass.findMethodByName(method);
            if(targetMethod == null) {
                return new PsiReference[0];
            }

            return new PsiReference[] {
                new ClassMethodStringPsiReference(psiElement, phpClass.getFQN(), targetMethod.getName()),
            };
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
            if(serviceClassTargets.size() == 0 && value.startsWith("%") && value.endsWith("%") && value.length() > 2 && ContainerCollectionResolver.getParameterNames(getElement().getProject()).contains(StringUtils.strip(value, "%"))) {
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

    /**
     * <service id="Foobar\Foo"/>
     */
    private static class ServiceIdWithoutParameterReference extends PsiPolyVariantReferenceBase<PsiElement> {

        @NotNull
        private final XmlAttributeValue psiElement;

        private ServiceIdWithoutParameterReference(@NotNull XmlAttributeValue psiElement) {
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

    /**
     * <factory class="FooBar" method="cre<caret>ate"/>
     */
    private class FactoryClassMethodPsiReferenceProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
            if(!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement instanceof XmlAttributeValue)) {
                return new PsiReference[0];
            }

            String method = ((XmlAttributeValue) psiElement).getValue();
            if(StringUtils.isBlank(method)) {
                return new PsiReference[0];
            }

            PhpClass phpClass = XmlHelper.getPhpClassForClassFactory((XmlAttributeValue) psiElement);
            if(phpClass == null) {
                return new PsiReference[0];
            }

            Method classMethod = phpClass.findMethodByName(method);
            if(classMethod == null) {
                return new PsiReference[0];
            }

            return new PsiReference[]{
                new ClassMethodStringPsiReference(psiElement, phpClass.getFQN(), classMethod.getName()),
            };
        }
    }

    private class ChainPsiReferenceProvider extends PsiReferenceProvider {
        @NotNull
        private final PsiReferenceProvider[] psiReferenceProviders;

        ChainPsiReferenceProvider(@NotNull PsiReferenceProvider... psiReferenceProviders) {
            this.psiReferenceProviders = psiReferenceProviders;
        }

        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
            Collection<PsiReference> psiReferences = new ArrayList<>();

            for (PsiReferenceProvider provider : this.psiReferenceProviders) {
                ContainerUtil.addAll(psiReferences, provider.getReferencesByElement(psiElement, processingContext));
            }

            return psiReferences.toArray(new PsiReference[psiReferences.size()]);
        }
    }

    private class ClassMethodStringPsiReference extends PsiPolyVariantReferenceBase<PsiElement> {
        @NotNull
        private final String aClass;

        @NotNull
        private final String method;

        ClassMethodStringPsiReference(@NotNull PsiElement psiElement, @NotNull String aClass, @NotNull String method) {
            super(psiElement);
            this.aClass = aClass;
            this.method = method;
        }

        @NotNull
        @Override
        public ResolveResult[] multiResolve(boolean b) {
            Method classMethod = PhpElementsUtil.getClassMethod(getElement().getProject(), aClass, method);
            if(classMethod == null) {
                return new ResolveResult[0];
            }

            return PsiElementResolveResult.createResults(classMethod);
        }

        @NotNull
        @Override
        public Object[] getVariants() {
            return new Object[0];
        }
    }
}