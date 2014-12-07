package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RouteSymbolContributor implements ChooseByNameContributor {

    @NotNull
    @Override
    public String[] getNames(Project project, boolean b) {

        Set<String> routeNames = new HashSet<String>();

        Map<String, Route> routes = RouteHelper.getCompiledRoutes(project);

        for (Route route : routes.values()) {
            routeNames.add(route.getName());
        }

        return ArrayUtil.toStringArray(routeNames);
    }

    @NotNull
    @Override
    public NavigationItem[] getItemsByName(String routeName, String s2, Project project, boolean b) {

        List<NavigationItem> navigationItems = new ArrayList<NavigationItem>();

        for (PsiElement psiElement : RouteHelper.getMethods(project, routeName)) {
            if(psiElement instanceof NavigationItem) {
                navigationItems.add(new NavigationItemEx(psiElement, routeName, Symfony2Icons.ROUTE, "Route"));
            }
        }

        return navigationItems.toArray(new NavigationItem[navigationItems.size()]);

    }

}
