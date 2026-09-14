package fr.adrienbrault.idea.symfony2plugin.config.xml.inspection

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.xml.XmlAttributeValue
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class XmlDuplicateParameterKeyInspection : XmlDuplicateServiceKeyInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is XmlAttributeValue) {
                    visitRoot(element, holder, "parameters", "parameter", "key", "Symfony: Duplicate Key")
                }

                super.visitElement(element)
            }
        }
    }
}
