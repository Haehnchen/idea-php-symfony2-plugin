package fr.adrienbrault.idea.symfony2plugin.markdown

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.Language
import com.jetbrains.twig.TwigFileType
import com.jetbrains.twig.TwigLanguage
import org.intellij.plugins.markdown.injection.CodeFenceLanguageProvider

private const val TWIG_INFO_STRING = "twig"

class TwigCodeFenceLanguageProvider : CodeFenceLanguageProvider {
    override fun getLanguageByInfoString(infoString: String): Language? =
        if (isTwigInfoString(infoString)) MarkdownTwigLanguage else null

    override fun getExtensionByInfoString(infoString: String): String? =
        if (isTwigInfoString(infoString)) TwigFileType.INSTANCE.defaultExtension else null

    override fun getCompletionVariantsForInfoString(parameters: CompletionParameters): List<LookupElement> = listOf(
        LookupElementBuilder.create(TWIG_INFO_STRING)
            .withIcon(TwigFileType.INSTANCE.icon)
            .withTypeText(TwigLanguage.INSTANCE.displayName, true),
    )
}

private fun isTwigInfoString(infoString: String): Boolean =
    infoString.equals(TWIG_INFO_STRING, ignoreCase = true) ||
        infoString.startsWith("$TWIG_INFO_STRING ", ignoreCase = true)
