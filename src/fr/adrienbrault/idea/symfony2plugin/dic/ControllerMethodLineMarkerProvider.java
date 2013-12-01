package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.ElementBase;
import com.intellij.util.ConstantFunction;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ControllerMethodLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    @Nullable
    public LineMarkerInfo collect(PsiElement psiElement) {

        if(!(psiElement instanceof Method)) {
            return null;
        }

        String methodName = ((Method) psiElement).getName();
        if(!methodName.endsWith("Action")) {
            return null;
        }

        ArrayList<GotoRelatedItem> gotoRelatedItems = new ArrayList<GotoRelatedItem>();

        attachRelatedTemplates(psiElement, gotoRelatedItems);
        attachRelatedRoutes(psiElement, gotoRelatedItems);

        if(gotoRelatedItems.size() == 0) {
            return null;
        }

        // only one item dont need popover
        if(gotoRelatedItems.size() == 1) {

            GotoRelatedItem gotoRelatedItem = gotoRelatedItems.get(0);

            // hell: find any possible small icon
            Icon icon = null;
            if(gotoRelatedItem instanceof RelatedPopupGotoLineMarker.PopupGotoRelatedItem) {
                icon = ((RelatedPopupGotoLineMarker.PopupGotoRelatedItem) gotoRelatedItem).getSmallIcon();
            }

            if(icon == null) {
               icon = Symfony2Icons.SYMFONY_LINE_MARKER;
            }

            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(icon).
                setTargets(gotoRelatedItems.get(0).getElement());

            String customName = gotoRelatedItems.get(0).getCustomName();
            if(customName != null) {
                builder.setTooltipText(customName);
            }

            return builder.createLineMarkerInfo(psiElement);
        }

        return new LineMarkerInfo<PsiElement>(psiElement, psiElement.getTextOffset(), Symfony2Icons.SYMFONY_LINE_MARKER, 6, new ConstantFunction("Related Files"), new RelatedPopupGotoLineMarker.NavigationHandler(gotoRelatedItems));
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> psiElements, @NotNull Collection<LineMarkerInfo> results) {

        for(PsiElement psiElement: psiElements) {
            LineMarkerInfo lineMarkerInfo = collect(psiElement);
            if(lineMarkerInfo != null) {
                results.add(lineMarkerInfo);
            }
        }

    }

    private void attachRelatedRoutes(PsiElement psiElement, ArrayList<GotoRelatedItem> gotoRelatedItems) {
        // find routes
        List<Route> routes = RouteHelper.getRoutesOnControllerAction(((Method) psiElement));
        if(routes != null) {
            for(Route route: routes) {
                PsiElement routeTarget = RouteHelper.getRouteNameTarget(psiElement.getProject(), route.getName());
                if(routeTarget != null) {
                    gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(routeTarget, route.getName()).withIcon(Symfony2Icons.ROUTE, Symfony2Icons.ROUTE_LINE_MARKER));
                }
            }
        }
    }

    private void attachRelatedTemplates(PsiElement psiElement, ArrayList<GotoRelatedItem> gotoRelatedItems) {

        // on @Template annotation
        PhpDocComment phpDocComment = ((Method) psiElement).getDocComment();
        if(phpDocComment != null) {
            PhpDocTag[] phpDocTags = phpDocComment.getTagElementsByName("@Template");
            for(PhpDocTag phpDocTag: phpDocTags) {
                for(PsiElement templateAnnotationTarget: TwigUtil.getTemplateAnnotationFiles(phpDocTag)) {
                    gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templateAnnotationTarget));
                }
            }
        }

        // on method name
        String templateName = TwigUtil.getControllerMethodShortcut((Method) psiElement);
        if(templateName != null) {
            for(PsiElement templateTarget: TwigHelper.getTemplatePsiElements(psiElement.getProject(), templateName)) {
                gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templateTarget, templateName));
            }
        }

    }

}
