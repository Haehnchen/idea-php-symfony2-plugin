package fr.adrienbrault.idea.symfony2plugin.vite

import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.twig.TwigTokenTypes
import com.jetbrains.twig.elements.TwigElementTypes
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern

private val REPRISE_FUNCTIONS = setOf(
    "reprise_entry_script_tags", "reprise_entry_link_tags",
    "reprise_entry_js_files", "reprise_entry_css_files", "reprise_entry_exists"
)

/** Shared, PSI-local recognition for completion, navigation and the Twig usage index. */
internal fun viteEntryPattern() = PlatformPatterns.or(
    TwigPattern.getPrintBlockOrTagFunctionPattern("vite_entry_link_tags", "vite_entry_script_tags"),
    PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT).with(object : PatternCondition<PsiElement>("repriseEntry") {
        override fun accepts(element: PsiElement, context: ProcessingContext): Boolean = isRepriseDefaultEntry(element)
    })
)

/**
 * Matches the entry string in Reprise calls:
 * - `{{ reprise_entry_script_tags('checkout') }}`
 * - `{{ reprise_entry_link_tags(entryName='checkout') }}`
 * - `{{ reprise_entry_css_files(build=null, entryName='checkout') }}`
 * - `{{ reprise_entry_link_tags(entryName: 'checkout', build: null) }}`
 * - `{% set files = reprise_entry_js_files('checkout', 'cdn', null) %}`
 * - `{% if reprise_entry_exists('checkout', null) %}{% endif %}`
 */
private fun isRepriseDefaultEntry(element: PsiElement): Boolean {
    val call = element.parent ?: return false
    if (call.node.elementType != TwigElementTypes.FUNCTION_CALL) return false

    val children = generateSequence(call.firstChild) { it.nextSibling }
        .filterNot { it.text.isBlank() }.toList()
    val functionName = children.firstOrNull()?.text ?: return false
    if (functionName !in REPRISE_FUNCTIONS) return false
    val opening = children.indexOfFirst { it.node.elementType == TwigTokenTypes.LBRACE }
    if (opening < 0) return false

    // Nested calls and hashes remain whole PSI nodes, so their commas are not separators.
    val arguments = mutableListOf<List<PsiElement>>()
    var current = mutableListOf<PsiElement>()
    for (child in children.drop(opening + 1)) {
        if (child.node.elementType == TwigTokenTypes.RBRACE) break
        if (child.node.elementType == TwigTokenTypes.COMMA) {
            arguments.add(current)
            current = mutableListOf()
        } else {
            current.add(child)
        }
    }
    arguments.add(current)

    var entry: List<PsiElement>? = null
    var build: List<PsiElement>? = null
    for ((index, argument) in arguments.withIndex()) {
        val named = argument.size >= 2 && argument[1].node.elementType in setOf(TwigTokenTypes.EQ, TwigTokenTypes.COLON)
        val name = if (named) argument[0].text else when (index) {
            0 -> "entryName"
            (if (functionName == "reprise_entry_exists") 1 else 2) -> "build"
            else -> null
        }
        val value = if (named) argument.drop(2) else argument
        when (name) {
            "entryName" -> entry = value
            "build" -> build = value
        }
    }

    // Named/dynamic builds need Reprise's YAML-to-config mapping; never use default entries for them.
    if (build != null && !(build.size == 1 && build[0].text == "null")) return false
    val value = entry ?: return false
    return value.size == 3 && value[1] == element &&
        value[0].node.elementType in setOf(TwigTokenTypes.SINGLE_QUOTE, TwigTokenTypes.DOUBLE_QUOTE) &&
        value[2].node.elementType == value[0].node.elementType
}
