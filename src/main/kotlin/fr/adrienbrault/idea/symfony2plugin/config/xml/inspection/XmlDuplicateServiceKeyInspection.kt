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
    val xmlAttribute = xmlAttributeValue.parent as? XmlAttribute ?: return
    if (tagName != xmlAttribute.name) {
        return
    }

    val xmlTag = xmlAttribute.parent ?: return
    val rootContextXmlTag = xmlTag.parent as? XmlTag ?: return
    if (child != xmlTag.name || root != rootContextXmlTag.name) {
        return
    }

    val value by lazy(LazyThreadSafetyMode.NONE) { xmlAttributeValue.value }
    val hasDuplicate = rootContextXmlTag.findSubTags(child)
        .asSequence()
        .filter { it.getAttributeValue(tagName) == value }
        .take(2)
        .count() == 2

    if (hasDuplicate) {
        holder.registerProblem(xmlAttributeValue, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
    }
}
