package fr.adrienbrault.idea.symfony2plugin.templating.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.twig.TwigTokenTypes
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import org.apache.commons.lang3.StringUtils

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
        private var filterPattern: ElementPattern<*>? = null
        private var applyFilterPattern: ElementPattern<*>? = null
        private var printBlockFunctionPattern: ElementPattern<*>? = null

        private var namedDeprecatedTokenParserTags: Map<String, String>? = null
        private var deprecatedFilters: Set<String>? = null
        private var deprecatedFunctions: Set<String>? = null

        override fun visitElement(element: PsiElement) {
            // {% tag %}
            if (element.node.elementType === TwigTokenTypes.TAG_NAME) {
                visitTagTokenName(element)
            }

            // {{ value|filter }}
            // {% apply filter %}
            if (getFilterPattern().accepts(element) || getApplyFilterPattern().accepts(element)) {
                visitFilter(element)
            }

            // {{ function() }}
            if (getPrintBlockFunctionPattern().accepts(element)) {
                visitFunction(element)
            }

            super.visitElement(element)
        }

        private fun visitTagTokenName(element: PsiElement) {
            var tagName = element.text

            if (StringUtils.isBlank(tagName)) {
                return
            }

            // {% endspaceless % }
            if (tagName.length > 3 && tagName.startsWith("end")) {
                tagName = tagName.substring(3)
            }

            if (namedDeprecatedTokenParserTags == null) {
                namedDeprecatedTokenParserTags = TwigUtil.getNamedDeprecatedTokenParserTags(element.project)
            }

            val deprecatedTokenParserTags = namedDeprecatedTokenParserTags ?: return
            if (deprecatedTokenParserTags.containsKey(tagName)) {
                val descriptionTemplate = deprecatedTokenParserTags[tagName]

                // "Deprecated" highlight is not visible, so we are going here for weak warning
                // WEAK_WARNING would be match; but not really visible
                holder.registerProblem(
                    element.parent,
                    descriptionTemplate ?: "Deprecated Twig tag",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        }

        private fun visitFilter(element: PsiElement) {
            val filterName = element.text

            if (StringUtils.isBlank(filterName)) {
                return
            }

            if (deprecatedFilters == null) {
                deprecatedFilters = TwigUtil.getDeprecatedFilters(element.project)
            }

            if (deprecatedFilters?.contains(filterName) == true) {
                holder.registerProblem(
                    element,
                    "Deprecated Twig filter",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        }

        private fun visitFunction(element: PsiElement) {
            val functionName = element.text

            if (StringUtils.isBlank(functionName)) {
                return
            }

            if (deprecatedFunctions == null) {
                deprecatedFunctions = TwigUtil.getDeprecatedFunctions(element.project)
            }

            if (deprecatedFunctions?.contains(functionName) == true) {
                holder.registerProblem(
                    element,
                    "Deprecated Twig function",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        }

        private fun getFilterPattern(): ElementPattern<*> {
            return filterPattern ?: TwigPattern.getFilterPattern().also { filterPattern = it }
        }

        private fun getApplyFilterPattern(): ElementPattern<*> {
            return applyFilterPattern ?: TwigPattern.getApplyFilterPattern().also { applyFilterPattern = it }
        }

        private fun getPrintBlockFunctionPattern(): ElementPattern<*> {
            return printBlockFunctionPattern ?: TwigPattern.getPrintBlockFunctionPattern().also { printBlockFunctionPattern = it }
        }
    }
}
