package fr.adrienbrault.idea.symfonyplugin.codeInsight;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface GotoCompletionProviderInterface {
    @NotNull
    Collection<LookupElement> getLookupElements();

    @NotNull
    Collection<PsiElement> getPsiTargets(PsiElement element);
}
