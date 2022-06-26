package fr.adrienbrault.idea.symfony2plugin.util.psi;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.util.psi.matcher.ArrayValueWithKeyAndMethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.psi.matcher.ArrayValueWithKeyAndNewExpressionMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.psi.matcher.NamedValueWithKeyAndNewExpressionMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpPsiMatcher {

    @Nullable
    public static ArrayValueWithKeyAndMethod.Result match(@NotNull PsiElement psiElement, @NotNull ArrayValueWithKeyAndMethod.Matcher matcher) {
        return ArrayValueWithKeyAndMethod.match(psiElement, matcher);
    }

    /**
     * new Foo(['message' => '<caret>'])
     */
    @Nullable
    public static ArrayValueWithKeyAndNewExpressionMatcher.Result match(@NotNull PsiElement psiElement, @NotNull ArrayValueWithKeyAndNewExpressionMatcher.Matcher matcher) {
        return ArrayValueWithKeyAndNewExpression.match(psiElement, matcher);
    }

    /**
     * new Foo(message: '<caret>')
     */
    @Nullable
    public static NamedValueWithKeyAndNewExpression.Result match(@NotNull PsiElement psiElement, @NotNull NamedValueWithKeyAndNewExpression.Matcher matcher) {
        return NamedValueWithKeyAndNewExpression.match(psiElement, matcher);
    }

    final public static class ArrayValueWithKeyAndMethod extends ArrayValueWithKeyAndMethodMatcher {}
    final public static class ArrayValueWithKeyAndNewExpression extends ArrayValueWithKeyAndNewExpressionMatcher {}
    final public static class NamedValueWithKeyAndNewExpression extends NamedValueWithKeyAndNewExpressionMatcher {}
}
