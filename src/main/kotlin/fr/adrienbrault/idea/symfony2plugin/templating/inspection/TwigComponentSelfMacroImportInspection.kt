package fr.adrienbrault.idea.symfony2plugin.templating.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.twig.TwigFile
import com.jetbrains.twig.TwigTokenTypes
import com.jetbrains.twig.elements.TwigElementTypes
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHtmlCompletionUtil

/**
 * Detects invalid {@code {% from _self import ... %}} usage inside Symfony UX Twig Components.
 *
 * Inside a component context, {@code _self} does not refer to the current template, so macro
 * imports via {@code _self} will silently fail at runtime.
 *
 * Invalid:
 * <pre>
 * {@code
 * <twig:Alert>
 *     {% from _self import message_formatter %}   <-- ERROR
 * </twig:Alert>
 * }
 * </pre>
 *
 * Valid:
 * <pre>
 * {@code
 * <twig:Alert>
 *     {% from 'path/to/template.html.twig' import message_formatter %}
 * </twig:Alert>
 * }
 * </pre>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see <a href="https://symfony.com/bundles/ux-twig-component/current/index.html#using-macros-in-components">Symfony UX Twig Components - Macros</a>
 */
open class TwigComponentSelfMacroImportInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val node = element.node
                if (node == null) {
                    super.visitElement(element)
                    return
                }

                // Match the RESERVED_ID token (_self) that appears in an import tag
                if (node.elementType === TwigTokenTypes.RESERVED_ID
                    && element.text == "_self"
                    && isInsideFromImportTag(element)
                    && isInsideComponentContext(element)
                ) {
                    holder.registerProblem(
                        element,
                        "Cannot use '_self' to import macros inside a Twig component. Use the full template path instead.",
                        ProblemHighlightType.GENERIC_ERROR
                    )
                }

                super.visitElement(element)
            }

            /**
             * Returns true if this {@code _self} token is the template source in a
             * {@code {% from _self import ... %}} statement.
             */
            private fun isInsideFromImportTag(element: PsiElement): Boolean {
                val parent = element.parent ?: return false
                val parentNode = parent.node
                return parentNode != null && parentNode.elementType === TwigElementTypes.IMPORT_TAG
            }

            /**
             * Returns true when the element is inside a Twig component context:
             * either an HTML-syntax component ({@code <twig:Name>}) or a
             * Twig-syntax component ({@code {% component 'Name' %}}).
             */
            private fun isInsideComponentContext(element: PsiElement): Boolean {
                val containingFile = element.containingFile
                if (containingFile !is TwigFile) {
                    return false
                }

                // Check for HTML component context via the HTML language view
                if (TwigHtmlCompletionUtil.isInsideHtmlComponentTag(element, containingFile)) {
                    return true
                }

                // Check for Twig {% component %} tag context
                return isInsideTwigComponentTag(element)
            }

            /**
             * Checks if the element is inside a {@code {% component '...' %}...{% endcomponent %}} block
             * by counting occurrences of component/endcomponent tags in the text before the element.
             */
            private fun isInsideTwigComponentTag(element: PsiElement): Boolean {
                val file = element.containingFile ?: return false

                val fileText = file.text
                val offset = element.textOffset
                if (offset <= 0 || offset > fileText.length) {
                    return false
                }

                val textBefore = fileText.substring(0, offset)

                // Count open and close component tags before the element position
                val openCount = countTagOccurrences(textBefore, "component")
                val closeCount = countTagOccurrences(textBefore, "endcomponent")

                return openCount > closeCount
            }

            private fun countTagOccurrences(text: String, tagName: String): Int {
                var count = 0
                var index = 0
                // Match {%- component or {% component (optional whitespace/dash)
                val pattern = "{%"
                while (text.indexOf(pattern, index).also { index = it } != -1) {
                    var remaining = index + pattern.length
                    // Skip optional whitespace and dash
                    while (remaining < text.length && (text[remaining] == ' ' || text[remaining] == '\t' || text[remaining] == '-')) {
                        remaining++
                    }
                    // Check if the tag name matches
                    if (text.startsWith(tagName, remaining)) {
                        val afterTag = remaining + tagName.length
                        // Must be followed by whitespace, -, or %}
                        if (afterTag >= text.length || text[afterTag].isWhitespace()
                            || text[afterTag] == '-' || text[afterTag] == '%'
                        ) {
                            count++
                        }
                    }
                    index++
                }
                return count
            }
        }
    }
}
