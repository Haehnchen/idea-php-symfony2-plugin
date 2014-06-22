package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.resolve.PhpResolveResult;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import org.jetbrains.annotations.NotNull;

public class RouteXmlReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar registrar) {

        registrar.registerReferenceProvider(
            Pattern.getArgumentServiceIdPattern(),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[] {
                        new RouteActionReference(psiElement)
                    };

                }
            }
        );

    }

    private static class RouteActionReference extends PsiPolyVariantReferenceBase<PsiElement> {

        public RouteActionReference(PsiElement psiElement) {
            super(psiElement);
        }

        @NotNull
        @Override
        public ResolveResult[] multiResolve(boolean b) {

            Method method = ControllerIndex.getControllerMethod(getElement().getProject(), getElement().getText());

            if(method != null) {
                return new ResolveResult[] {
                    new PhpResolveResult(method)
                };
            }

            return new ResolveResult[0];
        }

        @NotNull
        @Override
        public Object[] getVariants() {
            return ControllerIndex.getControllerLookupElements(getElement().getProject()).toArray();
        }
    }

    private static class Pattern {

        /**
         * <route id="foo" path="/blog/{slug}">
         *  <default key="_controller">Foo:Demo:hello</default>
         * </route>
         */
        public static PsiElementPattern.Capture<PsiElement> getArgumentServiceIdPattern() {
            return XmlPatterns
                .psiElement(XmlTokenType.XML_DATA_CHARACTERS)
                .withParent(XmlPatterns
                    .xmlText()
                    .withParent(XmlPatterns
                        .xmlTag()
                        .withName("default")
                        .withChild(
                            XmlPatterns.xmlAttribute().withName("key").withValue(
                                XmlPatterns.string().oneOfIgnoreCase("_controller")
                            )
                        )
                    )
                ).inside(
                    XmlHelper.getInsideTagPattern("route")
                ).inFile(XmlHelper.getXmlFilePattern());
        }
    }

}
