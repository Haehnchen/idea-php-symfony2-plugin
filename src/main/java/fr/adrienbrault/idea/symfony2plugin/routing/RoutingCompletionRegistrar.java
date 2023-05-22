package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProviderInterfaceEx;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RoutingCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        registrar.register(PhpElementsUtil.getParameterListArrayValuePattern(), psiElement -> {
            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            PsiElement context = psiElement.getContext();
            if (!(context instanceof StringLiteralExpression)) {
                return null;
            }

            MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.ArrayParameterMatcher(context, 1)
                .withSignature(PhpRouteReferenceContributor.GENERATOR_SIGNATURES)
                .match();

            if (methodMatchParameter == null) {
                return null;
            }

            String routeName = PsiElementUtils.getMethodParameterAt(methodMatchParameter.getMethodReference(), 0);
            if (routeName == null || routeName.isBlank()) {
                return null;
            }

            return new RouteParamaterGotoCompletionProvider((StringLiteralExpression) context, routeName);
        });
    }

    private static class RouteParamaterGotoCompletionProvider extends GotoCompletionProvider implements GotoCompletionProviderInterfaceEx {
        private final String routeName;

        public RouteParamaterGotoCompletionProvider(@NotNull StringLiteralExpression element, @NotNull String routeName) {
            super(element);
            this.routeName = routeName;
        }

        @Override
        public @NotNull Collection<LookupElement> getLookupElements() {
            return Arrays.asList(RouteHelper.getRouteParameterLookupElements(getElement().getProject(), routeName));
        }

        @Override
        public @NotNull Collection<PsiElement> getPsiTargets(PsiElement element) {
            String parameterName = GotoCompletionUtil.getStringLiteralValue(element);
            if (parameterName != null) {
                return Arrays.asList(RouteHelper.getRouteParameterPsiElements(getElement().getProject(), this.routeName, parameterName));
            }

            return Collections.emptyList();
        }
    }
}
