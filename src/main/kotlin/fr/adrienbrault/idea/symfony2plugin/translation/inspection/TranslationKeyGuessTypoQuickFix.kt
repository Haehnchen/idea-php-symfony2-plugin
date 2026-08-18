package fr.adrienbrault.idea.symfony2plugin.translation.inspection

import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil
import fr.adrienbrault.idea.symfony2plugin.util.AbstractGuessTypoQuickFix
import fr.adrienbrault.idea.symfony2plugin.util.SimilarSuggestionUtil

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TranslationKeyGuessTypoQuickFix(
    private val missingTranslationKey: String,
    private val translationDomain: String
) : AbstractGuessTypoQuickFix() {
    override fun getSuggestionLabel(): String = "Translation Key"

    override fun getSimilarItems(project: Project): List<String> {
        val translationKeys = TranslationUtil.getTranslationLookupElementsOnDomain(project, translationDomain)
            .map { it.lookupString }
            .toSet()

        return SimilarSuggestionUtil.findSimilarString(missingTranslationKey, translationKeys)
    }
}
