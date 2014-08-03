package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public interface GotoCompletionContributor {
    @Nullable
    public GotoCompletionProvider getProvider(@Nullable PsiElement psiElement);
}
