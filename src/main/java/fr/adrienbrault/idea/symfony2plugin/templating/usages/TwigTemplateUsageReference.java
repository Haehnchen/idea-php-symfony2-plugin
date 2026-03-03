package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A synthetic PsiReference for Twig template usages.
 * Wraps a source element (like an include tag) and resolves to the target TwigFile.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTemplateUsageReference extends PsiReferenceBase<PsiElement> {

    private final PsiElement targetElement;

    public TwigTemplateUsageReference(@NotNull PsiElement sourceElement, @NotNull PsiElement targetElement, @NotNull TextRange rangeInElement) {
        super(sourceElement, rangeInElement, false);
        this.targetElement = targetElement;
    }

    @Override
    public @Nullable PsiElement resolve() {
        return targetElement;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return EMPTY_ARRAY;
    }

    @Override
    public @NotNull String getCanonicalText() {
        if (targetElement instanceof com.intellij.psi.PsiFile) {
            return ((com.intellij.psi.PsiFile) targetElement).getName();
        }
        return targetElement.getText();
    }
}
