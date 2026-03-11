package fr.adrienbrault.idea.symfony2plugin.codeInspection.command

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpReturnType
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent

/**
 * Inspection to suggest adding an int return type to __invoke() method in Symfony Commands.
 *
 * Symfony 7.3+ invokable commands using #[AsCommand] attribute with __invoke() method
 * should declare int as return type for the exit code (e.g., Command::SUCCESS, Command::FAILURE).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class CommandInvokableReturnTypeInspection : LocalInspectionTool() {
    companion object {
        const val MESSAGE = "Symfony: Consider adding int return type to command __invoke()"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is PhpClass) {
                    visitPhpClass(element, holder)
                }
                super.visitElement(element)
            }
        }
    }

    private fun visitPhpClass(phpClass: PhpClass, holder: ProblemsHolder) {
        if (!hasAsCommandAttribute(phpClass)) {
            return
        }

        val invokeMethod = phpClass.findOwnMethodByName("__invoke") ?: return

        if (!hasIntReturnType(invokeMethod)) {
            val problemElement = invokeMethod.nameIdentifier
            holder.registerProblem(
                problemElement ?: invokeMethod,
                MESSAGE,
                ProblemHighlightType.WEAK_WARNING
            )
        }
    }

    private fun hasAsCommandAttribute(phpClass: PhpClass): Boolean =
        phpClass.getAttributes("\\Symfony\\Component\\Console\\Attribute\\AsCommand").isNotEmpty()

    private fun hasIntReturnType(method: Method): Boolean {
        val returnType = PhpPsiUtil.getChildByCondition(method, PhpReturnType.INSTANCEOF) as? PhpReturnType ?: return false
        val type = returnType.type
        if (type.isEmpty) return false

        return type.types.any { typeName: String ->
            val normalized = typeName.replace("\\", "").lowercase()
            normalized == "int" || normalized == "integer"
        }
    }
}
