package fr.adrienbrault.idea.symfony2plugin.routing.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlToken
import com.intellij.util.containers.ContainerUtil
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper
import org.jetbrains.yaml.psi.YAMLKeyValue

private fun registerYmlRoutePatternProblem(holder: ProblemsHolder, element: YAMLKeyValue) {
    val key: PsiElement? = element.key
    if (key == null) {
        return
    }

    val s = PsiElementUtils.trimQuote(element.keyText)
    if ("pattern" == s && YamlHelper.isRoutingFile(element.containingFile)) {
        // pattern: foo
        holder.registerProblem(key, "Pattern is deprecated; use path instead", ProblemHighlightType.LIKE_DEPRECATED)

    } else if (("_method" == s || "_scheme" == s) && YamlHelper.isRoutingFile(element.containingFile)) {
        // requirements: { _method: 'foo', '_scheme': 'foo' }
        val parentOfType = PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java)
        if (parentOfType != null && "requirements" == parentOfType.keyText) {
            holder.registerProblem(key, String.format("The '%s' requirement is deprecated", s), ProblemHighlightType.LIKE_DEPRECATED)
        }
    }
}

private fun registerRoutePatternProblem(holder: ProblemsHolder, xmlAttribute: XmlAttribute) {
    if ("pattern" != xmlAttribute.name) {
        return
    }

    val xmlTagRoute = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlAttribute, XmlTag::class.java, "route")
    if (xmlTagRoute != null && xmlAttribute.firstChild != null) {
        holder.registerProblem(xmlAttribute.firstChild!!, "Pattern is deprecated; use path instead", ProblemHighlightType.LIKE_DEPRECATED)
    }
}

private fun registerAttributeRequirementProblem(holder: ProblemsHolder, xmlAttributeValue: XmlAttributeValue, requirementAttribute: String) {
    if (xmlAttributeValue.value != requirementAttribute) {
        return
    }

    val xmlAttributeKey = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlAttributeValue, XmlAttribute::class.java, "key")
    if (xmlAttributeKey != null) {
        val xmlTagDefault = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlAttributeKey, XmlTag::class.java, "requirement")
        if (xmlTagDefault != null) {
            val xmlTagRoute = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlTagDefault, XmlTag::class.java, "route")
            if (xmlTagRoute != null) {
                // attach to attribute token only we dont want " or ' char included
                val target: PsiElement? = findAttributeValueToken(xmlAttributeValue, requirementAttribute)

                holder.registerProblem(if (target != null) target else xmlAttributeValue, String.format("The '%s' requirement is deprecated", requirementAttribute), ProblemHighlightType.LIKE_DEPRECATED)
            }
        }
    }
}

/**
 * Find child token which stores value
 *
 * XmlToken: "'"
 * XmlToken: "attributeText"
 * XmlToken: "'"
 */
private fun findAttributeValueToken(xmlAttributeValue: XmlAttributeValue, attributeText: String): PsiElement? {
    return ContainerUtil.find(xmlAttributeValue.children) { psiElement ->
        psiElement is XmlToken && attributeText == psiElement.text
    }
}

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class RouteSettingDeprecatedInspection {
    open class RouteSettingDeprecatedInspectionYaml : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element is YAMLKeyValue) {
                        registerYmlRoutePatternProblem(holder, element)
                    }

                    super.visitElement(element)
                }
            }
        }
    }

    open class RouteSettingDeprecatedInspectionXml : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element is XmlAttributeValue) {
                        registerAttributeRequirementProblem(holder, element, "_method")
                        registerAttributeRequirementProblem(holder, element, "_scheme")
                    } else if (element is XmlAttribute) {
                        registerRoutePatternProblem(holder, element)
                    }

                    super.visitElement(element)
                }
            }
        }
    }
}
