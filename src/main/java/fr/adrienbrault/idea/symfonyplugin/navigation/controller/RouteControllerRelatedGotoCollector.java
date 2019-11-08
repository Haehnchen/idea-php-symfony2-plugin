package fr.adrienbrault.idea.symfonyplugin.navigation.controller;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfonyplugin.Symfony2Icons;
import fr.adrienbrault.idea.symfonyplugin.dic.RelatedPopupGotoLineMarker;
import fr.adrienbrault.idea.symfonyplugin.extension.ControllerActionGotoRelatedCollector;
import fr.adrienbrault.idea.symfonyplugin.extension.ControllerActionGotoRelatedCollectorParameter;
import fr.adrienbrault.idea.symfonyplugin.routing.Route;
import fr.adrienbrault.idea.symfonyplugin.routing.RouteHelper;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteControllerRelatedGotoCollector implements ControllerActionGotoRelatedCollector {

    @Override
    public void collectGotoRelatedItems(ControllerActionGotoRelatedCollectorParameter parameter) {
        for(Route route: RouteHelper.getRoutesOnControllerAction(parameter.getMethod())) {
            PsiElement routeTarget = RouteHelper.getRouteNameTarget(parameter.getProject(), route.getName());
            if(routeTarget != null) {
                parameter.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(routeTarget, route.getName()).withIcon(Symfony2Icons.ROUTE, Symfony2Icons.ROUTE_LINE_MARKER));
            }
        }
    }

}
