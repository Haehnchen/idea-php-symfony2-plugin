package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public interface GotoCompletionRegistrarParameter {
    public void register(@NotNull ElementPattern<? extends PsiElement> pattern, GotoCompletionContributor contributor);
}
