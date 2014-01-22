package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.config.SymfonyPhpReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.routing.PhpRouteReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public class TwigFoldingBuilder extends FoldingBuilderEx {

    @NotNull
    @Override
    public FoldingDescriptor[] buildFoldRegions(@NotNull PsiElement psiElement, @NotNull Document document, boolean b) {

        if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return new FoldingDescriptor[0];
        }

        List<FoldingDescriptor> descriptors = new ArrayList<FoldingDescriptor>();

        if(Settings.getInstance(psiElement.getProject()).codeFoldingTwigRoute) {
            attachPathFoldingDescriptors(psiElement, descriptors);
        }

        return descriptors.toArray(new FoldingDescriptor[descriptors.size()]);
    }

    private void attachPathFoldingDescriptors(PsiElement psiElement, List<FoldingDescriptor> descriptors) {

        // find path calls in file
        PsiElement[] psiElements = PsiTreeUtil.collectElements(psiElement, new PsiElementFilter() {
            @Override
            public boolean isAccepted(PsiElement psiElement) {
                return TwigHelper.getAutocompletableRoutePattern().accepts(psiElement);
            }
        });

        if(psiElements.length == 0) {
            return;
        }

        FoldingGroup group = FoldingGroup.newGroup("route");
        Map<String,Route> routes = null;
        for(PsiElement psiElement1: psiElements) {

            // cache routes if we need them
            if(routes == null) {
                Symfony2ProjectComponent symfony2ProjectComponent = psiElement.getProject().getComponent(Symfony2ProjectComponent.class);
                routes = symfony2ProjectComponent.getRoutes();
            }

            String contents = PsiElementUtils.trimQuote(psiElement1.getText());
            if(contents.length() > 0 && routes.containsKey(contents)) {
                final Route route = routes.get(contents);

                final String url = RouteHelper.getRouteUrl(route.getTokens());
                if(url != null) {
                    descriptors.add(new FoldingDescriptor(psiElement1.getNode(),
                        new TextRange(psiElement1.getTextRange().getStartOffset(), psiElement1.getTextRange().getEndOffset()), group) {
                        @Nullable
                        @Override
                        public String getPlaceholderText() {
                            return url;
                        }
                    });
                }
            }

        }

    }

    @Nullable
    @Override
    public String getPlaceholderText(@NotNull ASTNode astNode) {
        return "...";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode astNode) {
        return true;
    }
}
