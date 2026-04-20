package fr.adrienbrault.idea.symfony2plugin.routing.usages

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.jetbrains.php.lang.psi.elements.PhpAttribute
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.routing.Route
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil
import org.apache.commons.lang3.StringUtils
import org.jetbrains.yaml.psi.YAMLCompoundValue
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * Shared route-usage helper methods.
 * This centralizes declaration detection, usage detection, and the labels used by the Find Usages UI.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class RouteUsageUtil private constructor() {
    companion object {
        /**
         * Normalizes a PSI element under the caret to the route declaration element that owns it.
         * Supported declarations are YAML keys, XML route ids, and the PHP #[Route] name string.
         */
        @JvmStatic
        fun getRouteDeclarationTarget(element: PsiElement): PsiElement? {
            getYamlRouteDeclarationTarget(element)?.let { return it }
            getXmlRouteDeclarationTarget(element)?.let { return it }
            getPhpRouteDeclarationTarget(element)?.let { return it }
            return null
        }

        /**
         * Extracts the logical route name from a declaration target, independent of YAML, XML, or PHP syntax.
         */
        @JvmStatic
        fun getRouteNameForDeclaration(element: PsiElement): String? {
            return when (val declarationTarget = getRouteDeclarationTarget(element)) {
                is YAMLKeyValue -> declarationTarget.keyText
                is XmlAttribute -> declarationTarget.value
                is StringLiteralExpression -> declarationTarget.contents
                else -> null
            }
        }

        /**
         * Returns the primary label shown for a route declaration in Find Usages.
         */
        @JvmStatic
        fun getPresentableText(element: PsiElement): String {
            val routeName = getRouteNameForDeclaration(element)
            return StringUtils.defaultIfBlank(routeName, element.text)
        }

        /**
         * Builds the gray location text shown after the route name, combining route path and project-relative file path.
         */
        @JvmStatic
        fun getLocationString(element: PsiElement): String? {
            val relativePath = getRelativeFilePath(element)
            val routePath = getRoutePath(element)
            if (StringUtils.isBlank(relativePath) && StringUtils.isBlank(routePath)) {
                return null
            }

            if (StringUtils.isBlank(routePath)) {
                return relativePath
            }

            if (StringUtils.isBlank(relativePath)) {
                return routePath
            }

            return "$routePath, $relativePath"
        }

        /**
         * Detects a route name when the caret is on a PHP or Twig usage string rather than on the declaration.
         */
        @JvmStatic
        fun getRouteNameForUsage(element: PsiElement): String? {
            for (candidate in getCandidateElements(element)) {
                if (candidate is StringLiteralExpression) {
                    return candidate.contents
                }

                if (candidate.node != null &&
                    TwigUtil.isValidStringWithoutInterpolatedOrConcat(candidate) &&
                    (TwigPattern.getAutocompletableRoutePattern().accepts(candidate) ||
                        ((TwigPattern.getTwigRouteComparePattern().accepts(candidate) ||
                            TwigPattern.getTwigRouteSameAsPattern().accepts(candidate) ||
                            TwigPattern.getTwigRouteInArrayPattern().accepts(candidate)) &&
                            TwigPattern.isRouteCompareContext(candidate)))
                ) {
                    return candidate.text
                }
            }

            return null
        }

        /**
         * Resolves the initial Find Usages targets for the caret location.
         * On declarations it returns the declaration target itself; on usage strings it resolves back to the owning declaration.
         */
        @JvmStatic
        fun getRouteSearchTargets(element: PsiElement): Collection<PsiElement> {
            for (candidate in getCandidateElements(element)) {
                val declarationTarget = getRouteDeclarationTarget(candidate)
                if (declarationTarget != null) {
                    return listOf(declarationTarget)
                }
            }

            val routeName = getRouteNameForUsage(element)
            if (StringUtils.isBlank(routeName)) {
                return emptyList()
            }

            return RouteHelper.getRouteNameTarget(element.project, routeName!!)
        }

        /**
         * Expands the caret context slightly so usage detection still works when IntelliJ gives a neighboring leaf node.
         */
        private fun getCandidateElements(element: PsiElement): Collection<PsiElement> {
            val elements = linkedSetOf(element)
            element.parent?.let(elements::add)

            if (element.containingFile == null || element.node == null) {
                return ArrayList(elements)
            }

            PsiTreeUtil.prevLeaf(element)?.let(elements::add)
            PsiTreeUtil.nextLeaf(element)?.let(elements::add)
            return ArrayList(elements)
        }

        /**
         * Checks whether the XML attribute under the caret is the id of a <route> declaration.
         */
        private fun isXmlRouteIdAttribute(xmlAttribute: XmlAttribute?): Boolean {
            if (xmlAttribute == null || xmlAttribute.name != "id") {
                return false
            }

            val parent = xmlAttribute.parent
            return parent != null && parent.name == "route"
        }

        private fun getYamlRouteDeclarationTarget(element: PsiElement): YAMLKeyValue? {
            val yamlKeyValue = when (element) {
                is YAMLKeyValue -> element
                else -> element.parent as? YAMLKeyValue
            }

            return yamlKeyValue?.takeIf {
                StringUtils.isNotBlank(it.keyText) && it.value is YAMLCompoundValue
            }
        }

        private fun getXmlRouteDeclarationTarget(element: PsiElement): XmlAttribute? {
            val xmlAttribute = when (element) {
                is XmlAttribute -> element
                else -> element.parent as? XmlAttribute
            }

            return xmlAttribute?.takeIf(::isXmlRouteIdAttribute)
        }

        private fun getPhpRouteDeclarationTarget(element: PsiElement): StringLiteralExpression? {
            val literalExpression = getPhpRouteNameLiteral(element) ?: return null
            val phpAttribute = PsiTreeUtil.getParentOfType(literalExpression, PhpAttribute::class.java, false) ?: return null

            return literalExpression.takeIf {
                isSupportedPhpAttributeDeclaration(literalExpression, phpAttribute)
            }
        }

        /**
         * Verifies that the PHP attribute under the caret is a supported #[Route] declaration and that the caret belongs to its name string.
         */
        private fun isSupportedPhpAttributeDeclaration(element: PsiElement, phpAttribute: PhpAttribute): Boolean {
            val fqn = phpAttribute.fqn
            if (fqn == null || !RouteHelper.isRouteClassAnnotation(fqn)) {
                return false
            }

            val routeName = PhpPsiAttributesUtil.getAttributeValueByNameAsString(phpAttribute, 1, "name")
            if (StringUtils.isBlank(routeName)) {
                return false
            }

            if (element is StringLiteralExpression) {
                return routeName == element.contents
            }

            val literalExpression = PsiTreeUtil.getParentOfType(element, StringLiteralExpression::class.java, false)
            return literalExpression != null && routeName == literalExpression.contents
        }

        private fun getPhpRouteNameLiteral(element: PsiElement): StringLiteralExpression? {
            return if (element is StringLiteralExpression) {
                element
            } else {
                PsiTreeUtil.getParentOfType(element, StringLiteralExpression::class.java, false)
            }
        }

        /**
         * Resolves the route path either from the indexed route model or directly from the declaration PSI as a fallback.
         */
        private fun getRoutePath(element: PsiElement): String? {
            val routeName = getRouteNameForDeclaration(element)
            if (StringUtils.isBlank(routeName)) {
                return extractInlineRoutePath(element)
            }

            for (route: Route in RouteHelper.getRoute(element.project, routeName!!)) {
                val path = route.path
                if (StringUtils.isNotBlank(path)) {
                    return path
                }
            }

            return extractInlineRoutePath(element)
        }

        /**
         * Extracts the inline path value directly from YAML, XML, or PHP attribute declarations when needed for presentation.
         */
        private fun extractInlineRoutePath(element: PsiElement): String? {
            val yamlKeyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java, false)
            if (yamlKeyValue != null) {
                for (child in PsiTreeUtil.findChildrenOfType(yamlKeyValue, YAMLKeyValue::class.java)) {
                    if (child.keyText == "path") {
                        return child.valueText
                    }
                }
            }

            val xmlAttribute = PsiTreeUtil.getParentOfType(element, XmlAttribute::class.java, false)
            if (xmlAttribute != null) {
                val xmlTag: XmlTag? = xmlAttribute.parent
                val path = xmlTag?.getAttributeValue("path")
                if (StringUtils.isNotBlank(path)) {
                    return path
                }
            }

            val phpAttribute = PsiTreeUtil.getParentOfType(element, PhpAttribute::class.java, false)
            if (phpAttribute != null) {
                val path = PhpPsiAttributesUtil.getAttributeValueByNameAsString(phpAttribute, 0, "path")
                if (StringUtils.isNotBlank(path)) {
                    return path
                }
            }

            return null
        }

        /**
         * Returns the project-relative file path used in Find Usages labels.
         */
        private fun getRelativeFilePath(element: PsiElement): String? {
            val file: PsiFile = element.containingFile ?: return null
            val virtualFile = file.virtualFile ?: return null

            return StringUtils.defaultIfBlank(
                VfsExUtil.getRelativeProjectPathStrict(element.project, virtualFile),
                VfsExUtil.getRelativeProjectPath(element.project, virtualFile),
            )
        }
    }
}
