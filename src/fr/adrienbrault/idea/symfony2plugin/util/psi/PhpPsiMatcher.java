package fr.adrienbrault.idea.symfony2plugin.util.psi;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.util.psi.matcher.ArrayValueWithKeyAndMethodMatcher;
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

    final public static class ArrayValueWithKeyAndMethod extends ArrayValueWithKeyAndMethodMatcher {}
}
