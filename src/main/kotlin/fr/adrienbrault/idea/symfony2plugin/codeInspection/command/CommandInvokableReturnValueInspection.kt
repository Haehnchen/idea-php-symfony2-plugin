package fr.adrienbrault.idea.symfony2plugin.codeInspection.command

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpReturn
import com.jetbrains.php.lang.psi.elements.PhpReturnType
import com.jetbrains.php.lang.psi.elements.PhpTypedElement
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent

/**
 * Inspection to detect invokable Symfony Commands that return non-integer values.
 *
 * Symfony 7.3+ invokable commands using #[AsCommand] attribute with __invoke() method
 * must return an integer exit code (e.g., Command::SUCCESS, Command::FAILURE).
 *
 * This inspection checks the actual return statements inside the method body.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class CommandInvokableReturnValueInspection : LocalInspectionTool() {
    companion object {
        const val MESSAGE = "Symfony: Command must return an integer value"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is PhpReturn) {
                    visitPhpReturn(element, holder)
                }
                super.visitElement(element)
            }
        }
    }

    private fun visitPhpReturn(phpReturn: PhpReturn, holder: ProblemsHolder) {
        val method = PsiTreeUtil.getParentOfType(phpReturn, Method::class.java) ?: return
        if (method.name != "__invoke") return

        if (hasIntReturnType(method)) return

        val phpClass: PhpClass = method.containingClass ?: return
        if (!hasAsCommandAttribute(phpClass)) return

        val argument = phpReturn.argument
        if (argument == null) {
            holder.registerProblem(phpReturn, MESSAGE, ProblemHighlightType.WARNING)
            return
        }

        if (!isIntegerType(argument, holder)) {
            holder.registerProblem(argument, MESSAGE, ProblemHighlightType.WARNING)
        }
    }

    private fun hasIntReturnType(method: Method): Boolean {
        val returnType = PhpPsiUtil.getChildByCondition(method, PhpReturnType.INSTANCEOF) as? PhpReturnType ?: return false
        val type = returnType.type
        if (type.isEmpty) return false

        return type.types.any { typeName: String ->
            val normalized = typeName.replace("\\", "").lowercase()
            normalized == "int" || normalized == "integer"
        }
    }

    private fun isIntegerType(element: PsiElement, holder: ProblemsHolder): Boolean {
        if (element !is PhpTypedElement) return false

        val type = element.type
        if (type.isEmpty) return false

        val phpIndex = PhpIndex.getInstance(holder.project)
        val completedType = phpIndex.completeType(holder.project, type, HashSet())

        return completedType.types.any { typeName ->
            val normalized = typeName.replace("\\", "").lowercase()
            normalized == "int" || normalized == "integer"
        }
    }

    private fun hasAsCommandAttribute(phpClass: PhpClass): Boolean =
        phpClass.getAttributes("\\Symfony\\Component\\Console\\Attribute\\AsCommand").isNotEmpty()
}
