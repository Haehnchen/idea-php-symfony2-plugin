package fr.adrienbrault.idea.symfony2plugin.templating.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.twig.TwigTokenTypes
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil

/**
 * Inspection for deprecated Twig extensions (tags, filters, and functions)
 *
 * Examples:
 * {% spaceless %} - deprecated tag
 * {{ value|deprecated_filter }} - deprecated filter
 * {{ deprecated_function() }} - deprecated function
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class TwigExtensionDeprecatedInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private val filterPattern by lazy(LazyThreadSafetyMode.NONE) { TwigPattern.getFilterPattern() }
        private val applyFilterPattern by lazy(LazyThreadSafetyMode.NONE) { TwigPattern.getApplyFilterPattern() }
        private val printBlockFunctionPattern by lazy(LazyThreadSafetyMode.NONE) { TwigPattern.getPrintBlockFunctionPattern() }
        private val namedDeprecatedTokenParserTags by lazy(LazyThreadSafetyMode.NONE) {
            TwigUtil.getNamedDeprecatedTokenParserTags(holder.project)
        }
        private val deprecatedFilters by lazy(LazyThreadSafetyMode.NONE) { TwigUtil.getDeprecatedFilters(holder.project) }
        private val deprecatedFunctions by lazy(LazyThreadSafetyMode.NONE) { TwigUtil.getDeprecatedFunctions(holder.project) }

        override fun visitElement(element: PsiElement) {
            // {% tag %}
            if (element.node.elementType === TwigTokenTypes.TAG_NAME) {
                visitTagTokenName(element)
            }

            // {{ value|filter }}
            // {% apply filter %}
            if (filterPattern.accepts(element) || applyFilterPattern.accepts(element)) {
                visitFilter(element)
            }

            // {{ function() }}
            if (printBlockFunctionPattern.accepts(element)) {
                visitFunction(element)
            }

            super.visitElement(element)
        }

        private fun visitTagTokenName(element: PsiElement) {
            var tagName = element.text.takeIf { it.isNotBlank() } ?: return

            // {% endspaceless % }
            if (tagName.length > 3 && tagName.startsWith("end")) {
                tagName = tagName.removePrefix("end")
            }

            if (tagName in namedDeprecatedTokenParserTags) {
                // "Deprecated" highlight is not visible, so we are going here for weak warning
                // WEAK_WARNING would be match; but not really visible
                holder.registerProblem(
                    element.parent,
                    namedDeprecatedTokenParserTags[tagName] ?: "Deprecated Twig tag",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        }

        private fun visitFilter(element: PsiElement) {
            val filterName = element.text

            if (filterName.isBlank()) {
                return
            }

            if (filterName in deprecatedFilters) {
                holder.registerProblem(
                    element,
                    "Deprecated Twig filter",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        }

        private fun visitFunction(element: PsiElement) {
            val functionName = element.text

            if (functionName.isBlank()) {
                return
            }

            if (functionName in deprecatedFunctions) {
                holder.registerProblem(
                    element,
                    "Deprecated Twig function",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        }

    }
}
