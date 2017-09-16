package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.form.gotoCompletion.TranslationDomainGotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.form.gotoCompletion.TranslationGotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpPsiMatcher;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ValidatorTranslationGotoCompletionRegistrar implements GotoCompletionRegistrar {
    private static final PhpPsiMatcher.ArrayValueWithKeyAndNewExpression.Matcher CONSTRAINT_MESSAGE = new PhpPsiMatcher.ArrayValueWithKeyAndNewExpression.Matcher(
        "message",
        "Symfony\\Component\\Validator\\Constraint"
    );

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        // new Constraint(['message' => '<caret>'])
        registrar.register(
            PhpPsiMatcher.ArrayValueWithKeyAndNewExpression.pattern(), psiElement -> {
                PsiElement parent = psiElement.getParent();
                if (!(parent instanceof StringLiteralExpression)) {
                    return null;
                }

                PhpPsiMatcher.ArrayValueWithKeyAndNewExpression.Result result = PhpPsiMatcher.match(parent, CONSTRAINT_MESSAGE);
                if (result == null) {
                    return null;
                }

                return new TranslationGotoCompletionProvider(psiElement, "validators");
            }
        );

        // addViolation('<caret>')
        // buildViolation('<caret>')
        registrar.register(
            PlatformPatterns.psiElement().withParent(PhpElementsUtil.getMethodWithFirstStringPattern()), psiElement -> {
                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterRecursiveMatcher(context, 0)
                    .withSignature("Symfony\\Component\\Validator\\Context\\ExecutionContextInterface", "addViolation")
                    .withSignature("Symfony\\Component\\Validator\\Context\\ExecutionContextInterface", "buildViolation") // @TODO: can have translation setter
                    .match();

                if (methodMatchParameter == null) {
                    return null;
                }

                return new TranslationGotoCompletionProvider(psiElement, "validators");
            }
        );

        // setTranslationDomain('<caret>')
        registrar.register(
            PlatformPatterns.psiElement().withParent(PhpElementsUtil.getMethodWithFirstStringPattern()), psiElement -> {
                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterRecursiveMatcher(context, 0)
                    .withSignature("\\Symfony\\Component\\Validator\\Violation\\ConstraintViolationBuilderInterface", "setTranslationDomain")
                    .match();

                if (methodMatchParameter == null) {
                    return null;
                }

                return new TranslationDomainGotoCompletionProvider(psiElement);
            }
        );
    }
}
