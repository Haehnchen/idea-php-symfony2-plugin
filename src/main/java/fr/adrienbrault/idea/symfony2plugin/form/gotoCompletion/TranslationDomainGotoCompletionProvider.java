package fr.adrienbrault.idea.symfony2plugin.form.gotoCompletion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Global translation domain
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationDomainGotoCompletionProvider extends GotoCompletionProvider {

    public TranslationDomainGotoCompletionProvider(@NotNull PsiElement element) {
        super(element);
    }

    @NotNull
    @Override
    public Collection<LookupElement> getLookupElements() {
        return TranslationUtil.getTranslationDomainLookupElements(getElement().getProject());
    }

    @NotNull
    @Override
    public Collection<PsiElement> getPsiTargets(PsiElement element) {
        String stringLiteralValue = GotoCompletionUtil.getStringLiteralValue(element);
        if(stringLiteralValue == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(TranslationUtil.getDomainPsiFiles(getElement().getProject(), stringLiteralValue));
    }
}
