package fr.adrienbrault.idea.symfony2plugin.translation.inspection;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.AbstractGuessTypoQuickFix;
import fr.adrienbrault.idea.symfony2plugin.util.SimilarSuggestionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationKeyGuessTypoQuickFix extends AbstractGuessTypoQuickFix {
    private final String missingTranslationKey;
    private final String translationDomain;

    public TranslationKeyGuessTypoQuickFix(@NotNull String missingTranslationKey, @NotNull String translationDomain) {
        this.missingTranslationKey = missingTranslationKey;
        this.translationDomain = translationDomain;
    }

    @Override
    protected @NotNull String getSuggestionLabel() {
        return "Translation Key";
    }

    @Override
    protected @NotNull List<String> getSimilarItems(@NotNull Project project) {
        Set<String> translationKeys = TranslationUtil.getTranslationLookupElementsOnDomain(project, translationDomain)
            .stream()
            .map(LookupElement::getLookupString)
            .collect(Collectors.toSet());

        return SimilarSuggestionUtil.findSimilarString(this.missingTranslationKey, translationKeys);
    }
}
