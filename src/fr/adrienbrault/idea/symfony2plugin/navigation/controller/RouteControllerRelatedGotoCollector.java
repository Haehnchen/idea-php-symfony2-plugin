package fr.adrienbrault.idea.symfony2plugin.navigation.controller;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollector;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;

import java.util.List;

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
