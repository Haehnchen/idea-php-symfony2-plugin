package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteSymbolContributor implements ChooseByNameContributorEx {

    @Override
    public void processNames(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter filter) {
        Project project = scope.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        Map<String, Route> routes = RouteHelper.getAllRoutes(project);

        for (Route route : routes.values()) {
            processor.process(route.getName());

            String path = route.getPath();
            if (path != null) {
                processor.process(path);
            }
        }
    }

    @Override
    public void processElementsWithName(@NotNull String name, @NotNull Processor<? super NavigationItem> processor, @NotNull FindSymbolParameters parameters) {
        Project project = parameters.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (PsiElement psiElement : RouteHelper.getMethods(project, name)) {
            processor.process((new NavigationItemEx(psiElement, name, Symfony2Icons.ROUTE, "Symfony Route")));
        }

        for (PsiElement psiElement : RouteHelper.getMethodsForPathWithPlaceholderMatch(project, name)) {
            processor.process((NavigationItemExStateless.create(psiElement, name, Symfony2Icons.ROUTE, "Symfony Route", true)));
        }
    }
}
