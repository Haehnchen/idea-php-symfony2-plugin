package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PhpRouteActionLineMarkerProvider extends RelatedItemLineMarkerProvider {

    protected void collectNavigationMarkers(@NotNull PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo> result) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return;
        }

        if(!PhpElementsUtil.getActionMethodPattern().accepts(psiElement)) {
            return;
        }

        PsiElement method = psiElement.getParent();
        if(!(method instanceof Method)) {
            return;
        }

        if(!((Method) method).getModifier().isPublic()) {
            return;
        }

        String controllerMethodName = RouteHelper.convertMethodToRouteControllerName((Method) method);
        if(controllerMethodName != null) {
            String[] routeNames = getRouteNamesOnControllerAction(psiElement.getProject(), controllerMethodName);
            if(routeNames != null) {
                for(String routeName: routeNames) {
                    attachTargets(psiElement, result, routeName);
                }
            }
        }

    }

    private static List<Route> getRouteOnControllerAction(Project project, String methodRouteActionName) {

        List<Route> routes = new ArrayList<Route>();

        Symfony2ProjectComponent symfony2ProjectComponent = project.getComponent(Symfony2ProjectComponent.class);
        for(Map.Entry<String, Route> routeEntry: symfony2ProjectComponent.getRoutes().entrySet()) {
            if(routeEntry.getValue().getController() != null && routeEntry.getValue().getController().equals(methodRouteActionName)) {
                routes.add(routeEntry.getValue());
            }
        }

        return routes;
    }

    @Nullable
    private static String[] getRouteNamesOnControllerAction(Project project, String methodRouteActionName) {

        ArrayList<String> names = new ArrayList<String>();

        for(Route route: getRouteOnControllerAction(project, methodRouteActionName)) {
            names.add(route.getName());
        }

        if(names.size() == 0) {
            return null;
        }

        return names.toArray(new String[names.size()]);
    }

    private void attachTargets(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo> result, String routeName) {

        VirtualFile[] virtualFiles = RouteHelper.getRouteDefinitionInsideFile(psiElement.getProject(), routeName);
        for(VirtualFile virtualFile: virtualFiles) {
            PsiFile psiFile = PsiManager.getInstance(psiElement.getProject()).findFile(virtualFile);
            if(psiFile != null) {

                YAMLKeyValue yamlKeyValue = YamlHelper.getRootKey(psiFile, routeName);

                NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.ROUTE_LINE_MARKER).
                        setTargets(yamlKeyValue).
                        setTooltipText("Navigate to definition");

                result.add(builder.createLineMarkerInfo(psiElement));
            }
        }
    }


}

