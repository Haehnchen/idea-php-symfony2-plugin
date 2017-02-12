package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteXmlReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar registrar) {

        registrar.registerReferenceProvider(
            XmlHelper.getRouteDefaultWithKeyAttributePattern("_controller"),
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
            return PsiElementResolveResult.createResults(
                RouteHelper.getMethodsOnControllerShortcut(getElement().getProject(), getElement().getText())
            );
        }

        @NotNull
        @Override
        public Object[] getVariants() {
            return ControllerIndex.getControllerLookupElements(getElement().getProject()).toArray();
        }
    }

}
