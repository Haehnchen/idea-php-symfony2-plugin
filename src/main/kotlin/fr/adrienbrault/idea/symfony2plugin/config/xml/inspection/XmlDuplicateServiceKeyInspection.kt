package fr.adrienbrault.idea.symfony2plugin.config.xml.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class XmlDuplicateServiceKeyInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is XmlAttributeValue) {
                    visitRoot(element, holder, "services", "service", "id", "Symfony: Duplicate Key")
                }

                super.visitElement(element)
            }
        }
    }
}

internal fun visitRoot(
    xmlAttributeValue: XmlAttributeValue,
    holder: ProblemsHolder,
    root: String,
    child: String,
    tagName: String,
    message: String
) {
    var value: String? = null

    val xmlAttribute = xmlAttributeValue.parent
    if (xmlAttribute is XmlAttribute && tagName == xmlAttribute.name) {
        val xmlTag = xmlAttribute.parent
        val rootContextXmlTag = xmlTag?.parent
        if (xmlTag != null && child == xmlTag.name && rootContextXmlTag is XmlTag && root == rootContextXmlTag.name) {
            var found = 0
            for (parameters in rootContextXmlTag.findSubTags(child)) {
                val key = parameters.getAttributeValue(tagName)

                // lazy value resolve
                if (value == null) {
                    value = xmlAttributeValue.value
                }

                if (value == key) {
                    found++
                }

                if (found == 2) {
                    holder.registerProblem(xmlAttributeValue, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                    break
                }
            }
        }
    }
}
