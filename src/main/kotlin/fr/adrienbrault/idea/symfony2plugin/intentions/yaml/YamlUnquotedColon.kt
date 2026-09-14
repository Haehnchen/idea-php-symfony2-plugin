package fr.adrienbrault.idea.symfony2plugin.intentions.yaml

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyUtil
import fr.adrienbrault.idea.symfony2plugin.util.VersionUtil.productVersionGreaterThanOrEqual
import org.jetbrains.yaml.YAMLElementTypes
import org.jetbrains.yaml.psi.YAMLCompoundValue
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class YamlUnquotedColon : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val project = holder.project

        if (!Symfony2ProjectComponent.isEnabled(project) || !SymfonyUtil.isVersionGreaterThenEquals(project, "2.8")) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        override fun visitElement(element: PsiElement) {
            // every array element implements this interface
            // check for inside "foo: <foo: foo>"
            if (!isIllegalColonExpression(element)) {
                super.visitElement(element)
                return
            }

            // element need to be inside key value
            val yamlKeyValue = element.parent
            if (yamlKeyValue !is YAMLKeyValue) {
                super.visitElement(element)
                return
            }

            // invalid inline item "foo: foo: foo", also check text length
            val text = yamlKeyValue.text
            if (!text.contains(": ") || text.contains("\n") || text.length > 200) {
                super.visitElement(element)
                return
            }

            // attach notification "foo: <foo: foo>"
            holder.registerProblem(
                element,
                MESSAGE,
                ProblemHighlightType.WEAK_WARNING
            )

            super.visitElement(element)
        }

        private fun isIllegalColonExpression(element: PsiElement): Boolean {
            if (productVersionGreaterThanOrEqual(2018, 3)) {
                return element is YAMLCompoundValue && element.node.elementType === YAMLElementTypes.MAPPING
            }

            return element is YAMLCompoundValue && element.node.elementType === YAMLElementTypes.COMPOUND_VALUE
        }
    }

    companion object {
        const val MESSAGE = "Using a colon in the unquoted mapping value is deprecated since Symfony 2.8 and will throw a ParseException in 3.0"
    }
}
