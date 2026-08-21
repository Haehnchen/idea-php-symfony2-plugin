package fr.adrienbrault.idea.symfony2plugin.routing.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import org.apache.commons.lang3.StringUtils

private fun inspectTwigRouteMissing(element: PsiElement, holder: ProblemsHolder) {
    val text = element.text
    if (StringUtils.isBlank(text)) {
        return
    }

    val routeName = RouteHelper.unescapeRouteName(text)
    if (!RouteHelper.isExistingRouteName(element.project, routeName)) {
        holder.registerProblem(element, "Symfony: Missing Route", RouteGuessTypoQuickFix(text))
        return
    }

    if (RouteHelper.isRouteControllerDeprecated(element.project, routeName)) {
        holder.registerProblem(element, "Symfony: Controller action is deprecated", ProblemHighlightType.LIKE_DEPRECATED)
    }
}

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class TwigRouteMissingInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private var autocompletableRoutePattern: ElementPattern<*>? = null

        override fun visitElement(element: PsiElement) {
            if (getAutocompletableRoutePattern().accepts(element) && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
                inspectTwigRouteMissing(element, holder)
            }

            super.visitElement(element)
        }

        private fun getAutocompletableRoutePattern(): ElementPattern<*> {
            if (autocompletableRoutePattern != null) {
                return autocompletableRoutePattern!!
            }

            autocompletableRoutePattern = TwigPattern.getAutocompletableRoutePattern()
            return autocompletableRoutePattern!!
        }
    }
}
