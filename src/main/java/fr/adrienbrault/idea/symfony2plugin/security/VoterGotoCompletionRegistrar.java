package fr.adrienbrault.idea.symfony2plugin.security;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.security.utils.VoterUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.apache.commons.lang3.StringUtils;
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
        // {% set voter_decision = access_decision('post_edit', post) %} (Symfony 7.4)
        registrar.register(
            PlatformPatterns.or(
                TwigPattern.getPrintBlockOrTagFunctionPattern("is_granted", "access_decision"),
                TwigPattern.getFunctionWithFirstParameterAsArrayPattern("is_granted", "access_decision"),
                TwigPattern.getFunctionWithFirstParameterAsLiteralPattern("is_granted", "access_decision")
            ),
            MyVisitorGotoCompletionProvider::new
        );

        // {% is_granted_for_user(user, 'foobar') %}
        // {% is_granted_for_user(user, {'foobar'}) %}
        // {% is_granted_for_user(user, ['foobar']) %}
        // {% set voter_decision = access_decision_for_user(user, 'post_edit', post) %} (Symfony 7.4)
        registrar.register(
            PlatformPatterns.or(
                TwigPattern.getPrintBlockOrTagFunctionSecondParameterPattern("is_granted_for_user", "access_decision_for_user"),
                TwigPattern.getFunctionWithSecondParameterAsArrayPattern("is_granted_for_user", "access_decision_for_user"),
                TwigPattern.getFunctionWithSecondParameterAsLiteralPattern("is_granted_for_user", "access_decision_for_user")
            ),
            MyVisitorGotoCompletionProvider::new
        );

        // Symfony\Component\Security\Core\Authorization\AuthorizationCheckerInterface::isGranted %}
        registrar.register(PlatformPatterns.psiElement().withParent(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE), psiElement -> {
            PsiElement context = psiElement.getContext();
            if (!(context instanceof StringLiteralExpression)) {
                return null;
            }

            if (!isIsGrantedMethodMatch((StringLiteralExpression) context)) {
                return null;
            }

            return new MyVisitorGotoCompletionProvider(psiElement);
        });
    }

    private static class MyVisitorGotoCompletionProvider extends GotoCompletionProvider {
        MyVisitorGotoCompletionProvider(PsiElement psiElement) {
            super(psiElement);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            return VoterUtil.getVoterAttributeLookupElements(getProject());
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

    public static boolean isIsGrantedMethodMatch(@NotNull StringLiteralExpression stringLiteralExpression) {
        MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterRecursiveMatcher(stringLiteralExpression, 0)
            .withSignature("Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "isGranted")
            .withSignature("Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "denyAccessUnlessGranted")

            // Symfony 3.3 / 3.4
            .withSignature("Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerTrait", "isGranted")
            .withSignature("Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerTrait", "denyAccessUnlessGranted")

            // Symfony 4 / 5 / 6
            .withSignature("Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "isGranted")
            .withSignature("Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "denyAccessUnlessGranted")

            .withSignature("Symfony\\Component\\Security\\Core\\Authorization\\AuthorizationCheckerInterface", "isGranted")
            .match();

        if (methodMatchParameter != null) {
            return true;
        }

        // Check for isGrantedForUser second parameter (index 1)
        MethodMatcher.MethodMatchParameter secondParameterMatch = new MethodMatcher.StringParameterRecursiveMatcher(stringLiteralExpression, 1)
            .withSignature("Symfony\\Component\\Security\\Core\\Authorization\\UserAuthorizationCheckerInterface", "isGrantedForUser")
            .match();

        if (secondParameterMatch != null) {
            return true;
        }

        MethodMatcher.MethodMatchParameter arrayMatchParameter = new MethodMatcher.ArrayParameterMatcher(stringLiteralExpression, 0)
            .withSignature("Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "isGranted")
            .withSignature("Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "denyAccessUnlessGranted")

            // Symfony 4
            .withSignature("Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "isGranted")
            .withSignature("Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "denyAccessUnlessGranted")

            .withSignature("Symfony\\Component\\Security\\Core\\Authorization\\AuthorizationCheckerInterface", "isGranted")
            .match();

        if (arrayMatchParameter != null) {
            return true;
        }

        // Check for isGrantedForUser second parameter as an array (index 1)
        MethodMatcher.MethodMatchParameter secondArrayParameterMatch = new MethodMatcher.ArrayParameterMatcher(stringLiteralExpression, 1)
            .withSignature("Symfony\\Component\\Security\\Core\\Authorization\\UserAuthorizationCheckerInterface", "isGrantedForUser")
            .match();

        return secondArrayParameterMatch != null;
    }
}
