package fr.adrienbrault.idea.symfony2plugin.routing.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import org.apache.commons.lang3.StringUtils

private fun inspectTwigRouteCompare(element: PsiElement, holder: ProblemsHolder) {
    val routeName = element.text
    if (StringUtils.isBlank(routeName)) {
        return
    }

    if (!RouteHelper.isExistingRouteName(element.project, routeName)) {
        holder.registerProblem(element, "Symfony: Missing Route", RouteGuessTypoQuickFix(routeName))
    }
}

/**
 * Inspects route names used in Twig comparison expressions against app.request.attributes.get('_route'):
 *
 *   app.request.attributes.get('_route') == 'my_route'
 *   app.request.attributes.get('_route') != 'my_route'
 *   app.request.attributes.get('_route') is same as('my_route')
 *   app.request.attributes.get('_route') in ['route_a', 'route_b']
 *   app.request.attributes.get('_route') not in ['route_a', 'route_b']
 *
 * Note: 'starts with' is intentionally excluded because the string is a prefix, not a full route name.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class TwigRouteCompareInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if ((TwigPattern.getTwigRouteComparePattern().accepts(element)
                        || TwigPattern.getTwigRouteSameAsPattern().accepts(element)
                        || TwigPattern.getTwigRouteInArrayPattern().accepts(element))
                    && TwigPattern.isRouteCompareContext(element)
                    && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)
                ) {
                    inspectTwigRouteCompare(element, holder)
                }

                super.visitElement(element)
            }
        }
    }
}
