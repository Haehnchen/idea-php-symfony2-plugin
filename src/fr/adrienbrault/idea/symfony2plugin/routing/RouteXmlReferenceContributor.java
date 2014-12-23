package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.resolve.PhpResolveResult;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class RouteXmlReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar registrar) {

        registrar.registerReferenceProvider(
            XmlHelper.getRouteConfigControllerPattern(),
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
            PsiElement[] methodsOnControllerShortcut = RouteHelper.getMethodsOnControllerShortcut(getElement().getProject(), getElement().getText());
            return PhpResolveResult.create(Arrays.asList(methodsOnControllerShortcut));
        }

        @NotNull
        @Override
        public Object[] getVariants() {
            return ControllerIndex.getControllerLookupElements(getElement().getProject()).toArray();
        }
    }

}
