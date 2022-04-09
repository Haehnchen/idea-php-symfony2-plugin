package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @deprecated Use core features
 */
public interface GotoCompletionContributor {
    @Nullable
    GotoCompletionProvider getProvider(@NotNull PsiElement psiElement);
}
