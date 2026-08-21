package fr.adrienbrault.idea.symfony2plugin.routing.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.routing.PhpRouteReferenceContributor
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import org.apache.commons.lang3.StringUtils

private fun inspectPhpRouteMissing(routeName: String, element: PsiElement, holder: ProblemsHolder) {
    val methodMatchParameter: MethodMatcher.MethodMatchParameter? = MethodMatcher.StringParameterMatcher(element, 0)
        .withSignature(PhpRouteReferenceContributor.GENERATOR_SIGNATURES)
        .match()

    if (methodMatchParameter == null) {
        return
    }

    if (!RouteHelper.isExistingRouteName(element.project, routeName)) {
        holder.registerProblem(element, "Symfony: Missing Route", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, RouteGuessTypoQuickFix(routeName))
        return
    }

    if (RouteHelper.isRouteControllerDeprecated(element.project, routeName)) {
        holder.registerProblem(element, "Symfony: Controller action is deprecated", ProblemHighlightType.LIKE_DEPRECATED)
    }
}

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class PhpRouteMissingInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private var methodWithFirstStringPattern: ElementPattern<*>? = null

        override fun visitElement(element: PsiElement) {
            if (element is StringLiteralExpression && getMethodWithFirstStringPattern().accepts(element)) {
                val contents = PhpElementsUtil.getStringValue(element)
                if (StringUtils.isNotBlank(contents)) {
                    inspectPhpRouteMissing(contents!!, element, holder)
                }
            }

            super.visitElement(element)
        }

        private fun getMethodWithFirstStringPattern(): ElementPattern<*> {
            if (methodWithFirstStringPattern != null) {
                return methodWithFirstStringPattern!!
            }

            methodWithFirstStringPattern = PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern()
            return methodWithFirstStringPattern!!
        }
    }
}
