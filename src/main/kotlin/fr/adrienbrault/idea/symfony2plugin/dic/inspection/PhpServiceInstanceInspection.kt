package fr.adrienbrault.idea.symfony2plugin.dic.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.ClassConstantReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceTypeHint
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil
import fr.adrienbrault.idea.symfony2plugin.dic.inspection.intention.PhpServiceSuggestIntentionAction
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil
import org.apache.commons.lang3.StringUtils

private fun registerPhpServiceInstanceProblem(
    argument: PsiElement,
    holder: ProblemsHolder,
    typeHint: ServiceTypeHint,
    lazyServiceCollector: ContainerCollectionResolver.LazyServiceCollector
) {
    val serviceId = if (argument is StringLiteralExpression) {
        val contents = argument.contents
        if (StringUtils.isBlank(contents)) {
            return
        }
        // Strip '@' prefix for raw '@service_id' strings
        if (contents.startsWith("@")) contents.substring(1) else contents
    } else if (argument is ClassConstantReference) {
        PhpElementsUtil.getClassConstantPhpFqn(argument)
    } else {
        return
    }

    if (serviceId == null || StringUtils.isBlank(serviceId)) {
        return
    }

    val serviceClass = ServiceUtil.getResolvedClassDefinition(holder.project, serviceId, lazyServiceCollector)
        ?: return

    val parameters = typeHint.method.parameters
    val index = typeHint.index
    if (index >= parameters.size) {
        return
    }

    val expectedClass = PhpElementsUtil.getClassInterface(holder.project, parameters[index].declaredType.toString())
        ?: return

    if (!PhpElementsUtil.isInstanceOf(serviceClass, expectedClass)) {
        holder.registerProblem(
            argument,
            "Expect instance of: " + expectedClass.presentableFQN,
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            PhpServiceSuggestIntentionAction(expectedClass.fqn, argument)
        )
    }
}

/**
 * PHP array-style and fluent service config inspection for wrong service instance references.
 *
 * <pre>
 * MyService::class => [
 *     'arguments' => [service('<caret>'), '@<caret>'],
 * ]
 *
 * $services->set(MyService::class)->args([service('<caret>')]);
 * </pre>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class PhpServiceInstanceInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private var lazyServiceCollector: ContainerCollectionResolver.LazyServiceCollector? = null

        override fun visitElement(psiElement: PsiElement) {
            if (psiElement is StringLiteralExpression) {
                val contents = psiElement.contents
                if (StringUtils.isNotBlank(contents)) {
                    visitArgument(psiElement)
                }
            } else if (psiElement is ClassConstantReference) {
                visitArgument(psiElement)
            }

            super.visitElement(psiElement)
        }

        private fun visitArgument(argument: PsiElement) {
            val collector = getLazyServiceCollector(holder.project)

            var typeHint: ServiceTypeHint? = ServiceContainerUtil.getPhpArrayConstructorTypeHint(argument, collector)
            if (typeHint == null) {
                typeHint = ServiceContainerUtil.getPhpFluentConstructorTypeHint(argument, collector)
            }

            if (typeHint == null) {
                return
            }

            registerPhpServiceInstanceProblem(argument, holder, typeHint, collector)
        }

        private fun getLazyServiceCollector(project: Project): ContainerCollectionResolver.LazyServiceCollector {
            val collector = lazyServiceCollector ?: ContainerCollectionResolver.LazyServiceCollector(project)
            lazyServiceCollector = collector
            return collector
        }
    }

}
