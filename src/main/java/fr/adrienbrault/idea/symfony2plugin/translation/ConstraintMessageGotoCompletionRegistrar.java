package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.form.gotoCompletion.TranslationGotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * class FooConstraint extends \Symfony\Component\Validator\Constraint
 * {
 *     public $message = 'This value should not be blank.';
 * }
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConstraintMessageGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        registrar.register(
            getConstraintPropertyMessagePattern(),
            new MyGotoCompletionContributor()
        );
    }

    @NotNull
    public static PsiElementPattern.Capture<PsiElement> getConstraintPropertyMessagePattern() {
        return PlatformPatterns.psiElement().withParent(PlatformPatterns.psiElement(StringLiteralExpression.class)
            .withParent(PlatformPatterns.psiElement(Field.class)
                .withName(PlatformPatterns.string().startsWith("message"))
            ));
    }

    private static class MyGotoCompletionContributor implements GotoCompletionContributor {
        @Nullable
        @Override
        public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {
            PsiElement parent = psiElement.getParent();

            if (parent instanceof StringLiteralExpression && TranslationUtil.isConstraintPropertyField((StringLiteralExpression) parent)) {
                return new TranslationGotoCompletionProvider(psiElement, "validators");
            }

            return null;
        }
    }
}
