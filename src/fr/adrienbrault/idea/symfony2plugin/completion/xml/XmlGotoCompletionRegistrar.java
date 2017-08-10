package fr.adrienbrault.idea.symfony2plugin.completion.xml;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.*;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.completion.DecoratedServiceCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteGotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateGotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlGotoCompletionRegistrar implements GotoCompletionRegistrar  {

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        // <import resource="config_foo.xml"/>
        registrar.register(
            XmlPatterns.psiElement().withParent(XmlHelper.getImportResourcePattern()),
            ImportResourceGotoCompletionProvider::new
        );

        // <service id="<caret>" class="MyFoo\Foo\Apple"/>
        registrar.register(
            XmlPatterns.psiElement().withParent(XmlHelper.getServiceIdAttributePattern()),
            ServiceIdCompletionProvider::new
        );

        // <factory class="AppBundle\Trivago\ConfigFactory" method="create"/>
        // <factory service="foo" method="create"/>
        registrar.register(
            XmlPatterns.psiElement().withParent(XmlHelper.getTagAttributePattern("factory", "method")
                .inside(XmlHelper.getInsideTagPattern("services"))
                .inFile(XmlHelper.getXmlFilePattern())),
            ServiceFactoryMethodCompletionProvider::new
        );

        // <default key="route">sonata_admin_dashboard</default>
        registrar.register(
            XmlHelper.getRouteDefaultWithKeyAttributePattern("route"),
            RouteGotoCompletionProvider::new
        );

        // <default key="template">foobar.html.twig</default>
        registrar.register(
            XmlHelper.getRouteDefaultWithKeyAttributePattern("template"),
            TemplateGotoCompletionRegistrar::new
        );

        // <service decorates="<caret>"/>
        registrar.register(
            XmlPatterns.psiElement().withParent(XmlHelper.getTagAttributePattern("service", "decorates")
                .inside(XmlHelper.getInsideTagPattern("services"))
                .inFile(XmlHelper.getXmlFilePattern())),
            MyDecoratedServiceCompletionProvider::new
        );

        // <foobar="@Foobar/profiler.html.twig" />
        registrar.register(
            XmlPatterns.psiElement().withParent(XmlHelper.getGlobalStringAttributePattern()),
            new MyGlobalStringTemplateGotoCompletionContributor()
        );

        // <foo template="@Foobar/profiler.html.twig" />
        registrar.register(
            XmlPatterns.psiElement().withParent(XmlHelper.getAttributePattern("template")),
            MyTemplateCompletionRegistrar::new
        );
    }

    private static class MyTemplateCompletionRegistrar extends TemplateGotoCompletionRegistrar{
        MyTemplateCompletionRegistrar(PsiElement element) {
            super(element);
        }

        @NotNull
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            return Collections.emptyList();
        }
    }

    private static class ImportResourceGotoCompletionProvider extends GotoCompletionProvider {

        ImportResourceGotoCompletionProvider(PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String xmlAttributeValue = GotoCompletionUtil.getXmlAttributeValue(element);
            if(xmlAttributeValue == null) {
                return Collections.emptyList();
            }

            Collection<PsiElement> targets = new ArrayList<>();

            targets.addAll(FileResourceUtil.getFileResourceTargetsInBundleScope(element.getProject(), xmlAttributeValue));
            targets.addAll(FileResourceUtil.getFileResourceTargetsInBundleDirectory(element.getProject(), xmlAttributeValue));

            PsiFile containingFile = element.getContainingFile();
            if(containingFile != null) {
                targets.addAll(FileResourceUtil.getFileResourceTargetsInDirectoryScope(containingFile, xmlAttributeValue));
            }

            return targets;
        }
    }

    private static class ServiceIdCompletionProvider extends GotoCompletionProvider {
        private ServiceIdCompletionProvider(PsiElement element) {
            super(element);
        }

        @Override
        public void getLookupElements(@NotNull GotoCompletionProviderLookupArguments arguments) {
            // find class name of service tag
            PsiElement xmlToken = this.getElement();
            if(xmlToken instanceof XmlToken) {
                PsiElement xmlAttrValue = xmlToken.getParent();
                if(xmlAttrValue instanceof XmlAttributeValue) {
                    PsiElement xmlAttribute = xmlAttrValue.getParent();
                    if(xmlAttribute instanceof XmlAttribute) {
                        PsiElement xmlTag = xmlAttribute.getParent();
                        if(xmlTag instanceof XmlTag) {
                            String aClass = ((XmlTag) xmlTag).getAttributeValue("class");
                            if(aClass == null) {
                                // <service id="Foo\Bar"/>

                                PhpClassCompletionProvider.addClassCompletion(
                                    arguments.getParameters(),
                                    arguments.getResultSet(),
                                    getElement(),
                                    false
                                );
                            } else if(StringUtils.isNotBlank(aClass)) {
                                // <service id="foo.bar" class="Foo\Bar"/>

                                LookupElementBuilder lookupElement = LookupElementBuilder
                                    .create(ServiceUtil.getServiceNameForClass(getProject(), aClass))
                                    .withIcon(Symfony2Icons.SERVICE);

                                arguments.getResultSet().addElement(lookupElement);
                            }
                        }
                    }
                }
            }
        }
    }

    private static class ServiceFactoryMethodCompletionProvider extends GotoCompletionProvider {
        ServiceFactoryMethodCompletionProvider(PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            PsiElement parent = getElement().getParent();
            if(!(parent instanceof XmlAttributeValue)) {
                return Collections.emptyList();
            }

            Collection<PhpClass> phpClasses = new ArrayList<>();

            ContainerUtil.addIfNotNull(phpClasses, XmlHelper.getPhpClassForClassFactory((XmlAttributeValue) parent));
            ContainerUtil.addIfNotNull(phpClasses, XmlHelper.getPhpClassForServiceFactory((XmlAttributeValue) parent));

            Collection<LookupElement> lookupElements = new ArrayList<>();

            for (PhpClass phpClass : phpClasses) {
                lookupElements.addAll(PhpElementsUtil.getClassPublicMethod(phpClass).stream()
                    .map(PhpLookupElement::new)
                    .collect(Collectors.toList())
                );
            }

            return lookupElements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            return Collections.emptyList();
        }
    }

    private static class MyDecoratedServiceCompletionProvider extends DecoratedServiceCompletionProvider {
        MyDecoratedServiceCompletionProvider(PsiElement psiElement) {
            super(psiElement);
        }

        @Nullable
        @Override
        public String findClassForElement(@NotNull PsiElement psiElement) {
            XmlTag parentOfType = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
            if (parentOfType != null) {
                String aClass = parentOfType.getAttributeValue("class");
                if (StringUtils.isNotBlank(aClass)) {
                    return aClass;
                }
            }

            return null;
        }

        @Nullable
        @Override
        public String findIdForElement(@NotNull PsiElement psiElement) {
            XmlTag parentOfType = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
            if(parentOfType == null) {
                return null;
            }

            return parentOfType.getAttributeValue("id");
        }
    }

    private static class MyGlobalStringTemplateGotoCompletionContributor implements GotoCompletionContributor {
        @Nullable
        @Override
        public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {
            return new GotoCompletionProvider(psiElement) {
                @NotNull
                @Override
                public Collection<LookupElement> getLookupElements() {
                    return Collections.emptyList();
                }

                @NotNull
                @Override
                public Collection<PsiElement> getPsiTargets(PsiElement element) {
                    String xmlAttributeValue = GotoCompletionUtil.getXmlAttributeValue(element);
                    if(xmlAttributeValue == null) {
                        return Collections.emptyList();
                    }

                    String s = xmlAttributeValue.toLowerCase();
                    if(!s.endsWith(".html.twig") && !s.endsWith(".html.php")) {
                        return Collections.emptyList();
                    }

                    return Arrays.asList(
                        TwigHelper.getTemplatePsiElements(getElement().getProject(), xmlAttributeValue)
                    );
                }
            };
        }
    }
}
