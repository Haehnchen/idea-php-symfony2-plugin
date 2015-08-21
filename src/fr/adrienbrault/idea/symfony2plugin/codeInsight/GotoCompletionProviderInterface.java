package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface GotoCompletionProviderInterface {

    @NotNull
    public Collection<LookupElement> getLookupElements();

    @NotNull
    public Collection<PsiElement> getPsiTargets(PsiElement element);

}
