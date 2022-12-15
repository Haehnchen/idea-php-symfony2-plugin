package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteSymbolContributor implements ChooseByNameContributor {

    @NotNull
    @Override
    public String @NotNull [] getNames(Project project, boolean b) {
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return new String[0];
        }

        Set<String> routeNames = new HashSet<>();

        Map<String, Route> routes = RouteHelper.getAllRoutes(project);

        for (Route route : routes.values()) {
            routeNames.add(route.getName());

            String path = route.getPath();
            if (path != null) {
                routeNames.add(path);
            }
        }

        return ArrayUtil.toStringArray(routeNames);
    }

    @NotNull
    @Override
    public NavigationItem @NotNull [] getItemsByName(String search, String s2, Project project, boolean b) {
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return new NavigationItem[0];
        }

        List<NavigationItem> navigationItems = new ArrayList<>();

        for (PsiElement psiElement : RouteHelper.getMethods(project, search)) {
            navigationItems.add(new NavigationItemEx(psiElement, search, psiElement.getIcon(0), "Symfony Route"));
        }

        for (PsiElement psiElement : RouteHelper.getMethodsForPathWithPlaceholderMatch(project, s2)) {
            navigationItems.add(new NavigationItemEx(psiElement, search, psiElement.getIcon(0), "Symfony Route"));
        }

        return navigationItems.toArray(new NavigationItem[0]);
    }
}
