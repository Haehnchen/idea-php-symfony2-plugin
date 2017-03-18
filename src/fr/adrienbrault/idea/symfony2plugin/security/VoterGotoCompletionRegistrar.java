package fr.adrienbrault.idea.symfony2plugin.security;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.security.utils.VoterUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class VoterGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        // {% is_granted('foobar') %}
        // {% is_granted({'foobar'}) %}
        // {% is_granted(['foobar']) %}
        registrar.register(
            PlatformPatterns.or(
                TwigHelper.getPrintBlockOrTagFunctionPattern("is_granted"),
                TwigHelper.getFunctionWithFirstParameterAsArrayPattern("is_granted"),
                TwigHelper.getFunctionWithFirstParameterAsLiteralPattern("is_granted")
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
            Collection<LookupElement> lookupElements = new ArrayList<>();

            Set<String> elements = new HashSet<>();

            VoterUtil.visitAttribute(getProject(), pair -> {
                String name = pair.getFirst();
                if(!elements.contains(name)) {
                    LookupElementBuilder lookupElement = LookupElementBuilder.create(name).withIcon(Symfony2Icons.SYMFONY);
                    PhpClass phpClass = PsiTreeUtil.getParentOfType(pair.getSecond(), PhpClass.class);
                    if(phpClass != null) {
                        lookupElement = lookupElement.withTypeText(phpClass.getName(), true);
                    }

                    lookupElements.add(lookupElement);
                    elements.add(name);
                }
            });

            return lookupElements;
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
