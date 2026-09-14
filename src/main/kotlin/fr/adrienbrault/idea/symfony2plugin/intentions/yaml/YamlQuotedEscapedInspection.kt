package fr.adrienbrault.idea.symfony2plugin.intentions.yaml

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiErrorElement
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyUtil
import org.jetbrains.yaml.YAMLTokenTypes

private val UNESCAPED_BACKSLASH_PATTERN = Regex(""".*[^\\]\\[^\\0abtnvfre "/N_LPxuU].*""")

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
                when (element.node.elementType) {
                    YAMLTokenTypes.SCALAR_DSTRING -> {
                        if (SymfonyUtil.isVersionGreaterThenEquals(project, "2.8")) {
                            // "Foo\Foo" -> "Foo\\Foo"
                            val text = element.text.trim('"')

                            // dont check to long strings
                            // ascii chars that need to be escape; some @see Symfony\Component\Yaml\Unescaper
                            if (text.length < 255 && UNESCAPED_BACKSLASH_PATTERN.matches(text)) {
                                holder.registerProblem(element, "Not escaping a backslash in a double-quoted string is deprecated", ProblemHighlightType.WEAK_WARNING)
                            }
                        }
                    }

                    YAMLTokenTypes.TEXT -> {
                        if (SymfonyUtil.isVersionGreaterThenEquals(project, "2.8")) {
                            // @foo -> "@foo"
                            val hasPreviousErrorElement = parentIsErrorAndHasPreviousElement(element)
                            val text = if (hasPreviousErrorElement) {
                                element.parent.prevSibling.text
                            } else {
                                element.text
                            }

                            if (text.length > 1 || hasPreviousErrorElement && text.isNotEmpty()) {
                                when (val startChar = text.first()) {
                                    '@', '`', '|', '>' -> holder.registerProblem(element, "Deprecated usage of '$startChar' at the beginning of unquoted string", ProblemHighlightType.WEAK_WARNING)

                                    '%' -> {
                                        // deprecated in => "3.1"; but as most user will need to migrate in 2.8 let them know it already
                                        holder.registerProblem(element, "Not quoting a scalar starting with the '%' indicator character is deprecated since Symfony 3.1", ProblemHighlightType.WEAK_WARNING)
                                    }
                                }
                            }
                        }
                    }
                }
                super.visitElement(element)
            }
        }
    }

    private fun parentIsErrorAndHasPreviousElement(element: PsiElement) =
        element.parent is PsiErrorElement && element.parent.prevSibling != null
}
