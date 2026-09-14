package fr.adrienbrault.idea.symfony2plugin.templating.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class TwigVariableDeprecatedInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private val typeCompletionPattern by lazy(LazyThreadSafetyMode.NONE) { TwigPattern.getTypeCompletionPattern() }

        override fun visitElement(element: PsiElement) {
            if (typeCompletionPattern.accepts(element)) {
                visit(element)
            }

            super.visitElement(element)
        }

        private fun visit(element: PsiElement) {
            val beforeLeaf = TwigTypeResolveUtil.formatPsiTypeName(element)
            if (beforeLeaf.isEmpty()) {
                return
            }

            val types = TwigTypeResolveUtil.resolveTwigMethodName(element, beforeLeaf)
            if (types.isEmpty()) {
                return
            }

            val text = element.text
            val visitedTargets = HashSet<String>()

            for (twigTypeContainer in types) {
                for (phpClass in TwigTypeResolveUtil.resolveTwigTypeClasses(element.project, twigTypeContainer)) {
                    for (namedElement in TwigTypeResolveUtil.getTwigPhpNameTargets(phpClass, text)) {
                        val targetKey = "${getDeprecatedTargetKind(namedElement)}:${phpClass.fqn}::${namedElement.name}"
                        if (visitedTargets.add(targetKey) && namedElement.isDeprecated) {
                            holder.registerProblem(element, getDeprecatedMessage(phpClass, namedElement), ProblemHighlightType.LIKE_DEPRECATED)
                        }
                    }
                }
            }
        }

        private fun getDeprecatedTargetKind(namedElement: PhpNamedElement) = when (namedElement) {
            is Method -> "method"
            is Field -> "field"
            else -> "element"
        }

        private fun getDeprecatedMessage(phpClass: PhpClass, namedElement: PhpNamedElement) =
            when (namedElement) {
                is Method -> "Method '${phpClass.name}::${namedElement.name}' is deprecated"
                is Field -> "Field '${phpClass.name}::$${namedElement.name}' is deprecated"
                else -> "Element '${phpClass.name}::${namedElement.name}' is deprecated"
            }
    }
}
