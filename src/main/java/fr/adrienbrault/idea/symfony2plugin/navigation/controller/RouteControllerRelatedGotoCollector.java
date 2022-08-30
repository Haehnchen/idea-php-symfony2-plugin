package fr.adrienbrault.idea.symfony2plugin.navigation.controller;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollector;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteControllerRelatedGotoCollector implements ControllerActionGotoRelatedCollector {

    @Override
    public void collectGotoRelatedItems(ControllerActionGotoRelatedCollectorParameter parameter) {
        for(Route route: RouteHelper.getRoutesOnControllerAction(parameter.getMethod())) {
            for (PsiElement psiElement : RouteHelper.getRouteNameTarget(parameter.getProject(), route.getName())) {
                parameter.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(psiElement, route.getName()).withIcon(Symfony2Icons.ROUTE, Symfony2Icons.ROUTE_LINE_MARKER));
            }
        }
    }

}
