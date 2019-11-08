package fr.adrienbrault.idea.symfonyplugin.security;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfonyplugin.security.utils.VoterUtil;
import fr.adrienbrault.idea.symfonyplugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfonyplugin.util.MethodMatcher;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class VoterGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        // {% is_granted('foobar') %}
        // {% is_granted({'foobar'}) %}
        // {% is_granted(['foobar']) %}
        registrar.register(
            PlatformPatterns.or(
                TwigPattern.getPrintBlockOrTagFunctionPattern("is_granted"),
                TwigPattern.getFunctionWithFirstParameterAsArrayPattern("is_granted"),
                TwigPattern.getFunctionWithFirstParameterAsLiteralPattern("is_granted")
            ),
            MyVisitorGotoCompletionProvider::new
        );

        // Symfony\Component\Security\Core\Authorization\AuthorizationCheckerInterface::isGranted %}
        registrar.register(PlatformPatterns.psiElement().withParent(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE), psiElement -> {
            PsiElement context = psiElement.getContext();
            if (!(context instanceof StringLiteralExpression)) {
                return null;
            }

            MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterRecursiveMatcher(context, 0)
                .withSignature("Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "isGranted")
                .withSignature("Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "denyAccessUnlessGranted")

                // Symfony 3.3 / 3.4
                .withSignature("Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerTrait", "isGranted")
                .withSignature("Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerTrait", "denyAccessUnlessGranted")

                .withSignature("Symfony\\Component\\Security\\Core\\Authorization\\AuthorizationCheckerInterface", "isGranted")
                .match();

            if(methodMatchParameter != null) {
                return new MyVisitorGotoCompletionProvider(psiElement);
            }

            // ['foobar']
            MethodMatcher.MethodMatchParameter arrayMatchParameter = new MethodMatcher.ArrayParameterMatcher(context, 0)
                .withSignature("Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "isGranted")
                .withSignature("Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "denyAccessUnlessGranted")
                .withSignature("Symfony\\Component\\Security\\Core\\Authorization\\AuthorizationCheckerInterface", "isGranted")
                .match();

            if(arrayMatchParameter != null) {
                return new MyVisitorGotoCompletionProvider(psiElement);
            }

            return null;
        });
    }

    private static class MyVisitorGotoCompletionProvider extends GotoCompletionProvider {
        MyVisitorGotoCompletionProvider(PsiElement psiElement) {
            super(psiElement);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            VoterUtil.LookupElementPairConsumer consumer = new VoterUtil.LookupElementPairConsumer();
            VoterUtil.visitAttribute(getProject(), consumer);
            return consumer.getLookupElements();
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String text = GotoCompletionUtil.getTextValueForElement(element);
            if(StringUtils.isBlank(text)) {
                return Collections.emptyList();
            }

            VoterUtil.TargetPairConsumer foo = new VoterUtil.TargetPairConsumer(text);
            VoterUtil.visitAttribute(getProject(), foo);
            return foo.getValues();
        }
    }
}
