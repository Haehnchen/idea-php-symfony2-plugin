package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.ElementBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ConstantFunction;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import icons.TwigIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

public class ControllerMethodLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    @Nullable
    public LineMarkerInfo collect(PsiElement psiElement) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        if(!(psiElement instanceof Method)) {
            return null;
        }

        String methodName = ((Method) psiElement).getName();
        if(!methodName.endsWith("Action")) {
            return null;
        }

        List<GotoRelatedItem> gotoRelatedItems = getGotoRelatedItems((Method) psiElement);

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

    public static List<GotoRelatedItem> getGotoRelatedItems(Method method) {
        List<GotoRelatedItem> gotoRelatedItems = new ArrayList<GotoRelatedItem>();

        attachRelatedTemplates(method, gotoRelatedItems);
        attachRelatedRoutes(method, gotoRelatedItems);
        return gotoRelatedItems;
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

    private static void attachRelatedRoutes(PsiElement psiElement, List<GotoRelatedItem> gotoRelatedItems) {
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

    private static void attachRelatedTemplates(Method method, List<GotoRelatedItem> gotoRelatedItems) {

        Set<String> uniqueTemplates = new HashSet<String>();

        // on @Template annotation
        PhpDocComment phpDocComment = method.getDocComment();
        if(phpDocComment != null) {
            PhpDocTag[] phpDocTags = phpDocComment.getTagElementsByName("@Template");
            for(PhpDocTag phpDocTag: phpDocTags) {
                for(Map.Entry<String, PsiElement> entry: TwigUtil.getTemplateAnnotationFiles(phpDocTag).entrySet()) {
                    if(!uniqueTemplates.contains(entry.getKey())) {
                        gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(entry.getValue(), entry.getKey()).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
                        uniqueTemplates.add(entry.getKey());
                    }
                }
            }
        }



        // on method name
        String templateName = TwigUtil.getControllerMethodShortcut(method);
        if(templateName != null) {
            for(PsiElement templateTarget: TwigHelper.getTemplatePsiElements(method.getProject(), templateName)) {
                if(!uniqueTemplates.contains(templateName)) {
                    gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templateTarget, templateName).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
                    uniqueTemplates.add(templateName);
                }
            }
        }

        // inside method
        for(MethodReference methodReference : PsiTreeUtil.findChildrenOfType(method, MethodReference.class)) {
            if(new Symfony2InterfacesUtil().isTemplatingRenderCall(methodReference)) {
                PsiElement templateParameter = PsiElementUtils.getMethodParameterPsiElementAt((methodReference).getParameterList(), 0);
                if(templateParameter != null) {
                    String resolveString = PhpElementsUtil.getStringValue(templateParameter);
                    if(resolveString != null && !uniqueTemplates.contains(resolveString)) {
                        uniqueTemplates.add(resolveString);
                        for(PsiElement templateTarget: TwigHelper.getTemplatePsiElements(method.getProject(), resolveString)) {
                            gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templateTarget, resolveString).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
                        }
                    }
                }
            }
        }


    }

}
