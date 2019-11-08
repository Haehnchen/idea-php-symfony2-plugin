package fr.adrienbrault.idea.symfonyplugin.codeInsight;

import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface GotoCompletionRegistrarParameter {
    void register(@NotNull ElementPattern<? extends PsiElement> pattern, GotoCompletionContributor contributor);
}
