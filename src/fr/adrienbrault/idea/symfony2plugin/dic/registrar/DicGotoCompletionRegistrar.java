package fr.adrienbrault.idea.symfony2plugin.dic.registrar;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterLookupElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DicGotoCompletionRegistrar implements GotoCompletionRegistrar {

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {

        // getParameter('FOO')
        registrar.register(
            XmlPatterns.psiElement().withParent(PhpElementsUtil.getMethodWithFirstStringPattern()), psiElement -> {

                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterRecursiveMatcher(context, 0)
                    .withSignature("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "hasParameter")
                    .withSignature("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "getParameter")
                    .match();

                if(methodMatchParameter == null) {
                    return null;
                }

                return new ParameterContributor((StringLiteralExpression) context);
            }
        );

    }

    private static class ParameterContributor extends GotoCompletionProvider {

        public ParameterContributor(StringLiteralExpression element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> results = new ArrayList<>();

            for(Map.Entry<String, ContainerParameter> entry: ContainerCollectionResolver.getParameters(getElement().getProject()).entrySet()) {
                results.add(new ParameterLookupElement(entry.getValue()));
            }

            return results;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = GotoCompletionUtil.getStringLiteralValue(element);
            if(contents == null) {
                return Collections.emptyList();
            }

            return ServiceUtil.getParameterDefinition(element.getProject(), contents);
        }
    }
}
