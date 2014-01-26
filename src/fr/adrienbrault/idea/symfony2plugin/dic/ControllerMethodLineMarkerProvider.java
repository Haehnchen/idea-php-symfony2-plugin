package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ConstantFunction;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.config.SymfonyPhpReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import icons.TwigIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;

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


        // inside method
        PsiElement[] methodParameter = PsiTreeUtil.collectElements(method, new PsiElementFilter() {
            @Override
            public boolean isAccepted(PsiElement psiElement) {
                return psiElement.getParent() instanceof ParameterList;
            }
        });

        attachRelatedTemplates(method, methodParameter, gotoRelatedItems);
        attachRelatedRoutes(method, gotoRelatedItems);
        attachRelatedModels(method, methodParameter, gotoRelatedItems);

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

    private static void attachRelatedTemplates(Method method, PsiElement[] parameterValues, List<GotoRelatedItem> gotoRelatedItems) {

        Set<String> uniqueTemplates = new HashSet<String>();

        // on @Template annotation
        PhpDocComment phpDocComment = method.getDocComment();
        if(phpDocComment != null) {
            PhpDocTag[] phpDocTags = phpDocComment.getTagElementsByName("@Template");
            for(PhpDocTag phpDocTag: phpDocTags) {
                for(Map.Entry<String, PsiElement> entry: TwigUtil.getTemplateAnnotationFiles(phpDocTag).entrySet()) {
                    if(!uniqueTemplates.contains(entry.getKey())) {
                        gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(entry.getValue(), TwigUtil.getFoldingTemplateNameOrCurrent(entry.getKey())).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
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
                    gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templateTarget, TwigUtil.getFoldingTemplateNameOrCurrent(templateName)).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
                    uniqueTemplates.add(templateName);
                }
            }
        }

        for(PsiElement psiElement: parameterValues) {
            MethodMatcher.MethodMatchParameter matchedSignature = MethodMatcher.getMatchedSignatureWithDepth(psiElement, SymfonyPhpReferenceContributor.TEMPLATE_SIGNATURES);
            if (matchedSignature != null) {
                String resolveString = PhpElementsUtil.getStringValue(psiElement);
                if(resolveString != null && !uniqueTemplates.contains(resolveString)) {
                    uniqueTemplates.add(resolveString);
                    for(PsiElement templateTarget: TwigHelper.getTemplatePsiElements(method.getProject(), resolveString)) {
                        gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templateTarget, resolveString).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
                    }
                }
            }

        }


    }

    private static void attachRelatedModels(Method method, PsiElement[] parameterValues, List<GotoRelatedItem> gotoRelatedItems) {

        List<PsiElement> uniqueTargets = new ArrayList<PsiElement>();

        for(PsiElement psiElement: parameterValues) {
            MethodMatcher.MethodMatchParameter matchedSignature = MethodMatcher.getMatchedSignatureWithDepth(psiElement, SymfonyPhpReferenceContributor.REPOSITORY_SIGNATURES);
            if (matchedSignature != null) {
                String resolveString = PhpElementsUtil.getStringValue(psiElement);
                if(resolveString != null)  {
                    for(PsiElement templateTarget: EntityHelper.getModelPsiTargets(method.getProject(), resolveString)) {

                        if(!uniqueTargets.contains(templateTarget)) {

                            uniqueTargets.add(templateTarget);
                            // we can provide targets to model config and direct class targets
                            if(templateTarget instanceof PsiFile) {
                                gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templateTarget, resolveString).withIcon(templateTarget.getIcon(0), Symfony2Icons.SYMFONY_LINE_MARKER));
                            } else {
                                // @TODO: we can resolve for model types and provide icons, but not for now
                                gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templateTarget, resolveString).withIcon(Symfony2Icons.DOCTRINE, Symfony2Icons.SYMFONY_LINE_MARKER));
                            }
                        }

                    }
                }
            }

        }

    }

}
