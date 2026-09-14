package fr.adrienbrault.idea.symfony2plugin.intentions.yaml

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiErrorElement
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyUtil
import org.apache.commons.lang3.StringUtils
import org.jetbrains.yaml.YAMLTokenTypes

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class YamlQuotedEscapedInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val project = holder.project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.node.elementType === YAMLTokenTypes.SCALAR_DSTRING && SymfonyUtil.isVersionGreaterThenEquals(project, "2.8")) {
                    // "Foo\Foo" -> "Foo\\Foo"
                    val text = StringUtils.strip(element.text, "\"")

                    // dont check to long strings
                    // ascii chars that need to be escape; some @see Symfony\Component\Yaml\Unescaper
                    if (text.length < 255 && text.matches(".*[^\\\\]\\\\[^\\\\0abtnvfre \"/N_LPxuU].*".toRegex())) {
                        holder.registerProblem(element, "Not escaping a backslash in a double-quoted string is deprecated", ProblemHighlightType.WEAK_WARNING)
                    }
                } else if (element.node.elementType === YAMLTokenTypes.TEXT && SymfonyUtil.isVersionGreaterThenEquals(project, "2.8")) {
                    // @foo -> "@foo"
                    val text = if (parentIsErrorAndHasPreviousElement(element)) {
                        element.parent.prevSibling.text
                    } else {
                        element.text
                    }

                    if (text.length > 1 || (parentIsErrorAndHasPreviousElement(element) && text.isNotEmpty())) {
                        val startChar = text.substring(0, 1)
                        if (startChar == "@" || startChar == "`" || startChar == "|" || startChar == ">") {
                            holder.registerProblem(element, String.format("Deprecated usage of '%s' at the beginning of unquoted string", startChar), ProblemHighlightType.WEAK_WARNING)
                        } else if (startChar == "%") {
                            // deprecated in => "3.1"; but as most user will need to migrate in 2.8 let them know it already
                            holder.registerProblem(element, "Not quoting a scalar starting with the '%' indicator character is deprecated since Symfony 3.1", ProblemHighlightType.WEAK_WARNING)
                        }
                    }
                }
                super.visitElement(element)
            }
        }
    }

    private fun parentIsErrorAndHasPreviousElement(element: PsiElement): Boolean {
        val parent = element.parent

        return parent is PsiErrorElement && parent.prevSibling != null
    }
}
