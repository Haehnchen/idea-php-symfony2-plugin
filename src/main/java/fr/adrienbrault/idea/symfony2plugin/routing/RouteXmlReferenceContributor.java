package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlText;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteXmlReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
            XmlHelper.getRouteControllerPattern(),
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
        private RouteActionReference(PsiElement psiElement) {
            super(psiElement);
        }

        @NotNull
        @Override
        public ResolveResult[] multiResolve(boolean b) {
            PsiElement element = getElement();
            PsiElement parent = element.getParent();

            String text = null;
            if(parent instanceof XmlText) {
                // <route><default key="_controller">Fo<caret>o\Bar</default></route>
                text = parent.getText();
            } else if(parent instanceof XmlAttribute) {
                // <route controller=""/>
                text = ((XmlAttribute) parent).getValue();
            }

            if(text == null || StringUtils.isBlank(text)) {
                return new ResolveResult[0];
            }

            return PsiElementResolveResult.createResults(
                RouteHelper.getMethodsOnControllerShortcut(getElement().getProject(), text)
            );
        }

        @NotNull
        @Override
        public Object[] getVariants() {
            return ControllerIndex.getControllerLookupElements(getElement().getProject()).toArray();
        }
    }
}
