package fr.adrienbrault.idea.symfony2plugin.navigation.controller

import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollector
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class RouteControllerRelatedGotoCollector : ControllerActionGotoRelatedCollector {
    override fun collectGotoRelatedItems(parameter: ControllerActionGotoRelatedCollectorParameter) {
        for (route in RouteHelper.getRoutesOnControllerAction(parameter.method)) {
            for (psiElement in RouteHelper.getRouteNameTarget(parameter.project, route.name)) {
                parameter.add(
                    RelatedPopupGotoLineMarker.PopupGotoRelatedItem(psiElement, route.name)
                        .withIcon(Symfony2Icons.ROUTE, Symfony2Icons.ROUTE_LINE_MARKER),
                )
            }
        }
    }
}
