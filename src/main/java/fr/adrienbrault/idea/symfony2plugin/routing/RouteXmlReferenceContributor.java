package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlText;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteXmlReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.and(XmlHelper.XML_EXTENSION, XmlHelper.getRouteControllerPattern()),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[] {
                        new RouteActionReference(psiElement)
                    };
                }

                @Override
                public boolean acceptsTarget(@NotNull PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
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
        public ResolveResult @NotNull [] multiResolve(boolean b) {
            PsiElement parent = getElement().getParent();
            if (parent == null) {
                return new ResolveResult[0];
            }

            String text = getControllerText(parent);
            if(text == null) {
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

    /**
     * <default key="_controller">Fo<caret>o\Bar</default>
     * <route controller=""/>
     */
    @Nullable
    public static String getControllerText(@NotNull PsiElement parent) {
        String text = null;
        if(parent instanceof XmlText) {
            // <route><default key="_controller">Fo<caret>o\Bar</default></route>
            text = parent.getText();
        } else if(parent instanceof XmlAttribute) {
            // <route controller=""/>
            text = ((XmlAttribute) parent).getValue();
        }

        if (StringUtils.isBlank(text)) {
            return null;
        }

        return text;
    }
}
