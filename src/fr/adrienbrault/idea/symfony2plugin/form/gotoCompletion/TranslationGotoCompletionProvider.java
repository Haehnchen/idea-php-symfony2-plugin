package fr.adrienbrault.idea.symfony2plugin.form.gotoCompletion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Global translation domain
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationGotoCompletionProvider extends GotoCompletionProvider {

    @NotNull
    private final String domain;

    public TranslationGotoCompletionProvider(@NotNull PsiElement element, @NotNull String domain) {
        super(element);
        this.domain = domain;
    }

    @NotNull
    @Override
    public Collection<LookupElement> getLookupElements() {
        return TranslationUtil.getTranslationLookupElementsOnDomain(getElement().getProject(), domain);
    }

    @NotNull
    @Override
    public Collection<PsiElement> getPsiTargets(PsiElement element) {
        String contents = GotoCompletionUtil.getStringLiteralValue(element);
        if(contents == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(TranslationUtil.getTranslationPsiElements(getProject(), contents, domain));
    }
}