package fr.adrienbrault.idea.symfony2plugin.extension;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface TwigFileUsage {
    Collection<String> getExtendsTemplate(@NotNull PsiElement psiElement);

    Collection<String> getIncludeTemplate(@NotNull PsiElement psiElement);

    boolean isExtendsTemplate(@NotNull PsiElement psiElement);
    boolean isIncludeTemplate(@NotNull PsiElement psiElement);
}
