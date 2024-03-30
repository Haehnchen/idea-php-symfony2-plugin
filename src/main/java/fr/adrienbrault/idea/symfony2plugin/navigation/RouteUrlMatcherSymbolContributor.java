package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.ChooseByNameContributorEx2;
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
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteUrlMatcherSymbolContributor implements ChooseByNameContributorEx, ChooseByNameContributorEx2 {

    @Override
    public void processNames(@NotNull Processor<? super String> processor, @NotNull FindSymbolParameters parameters) {
        Project project = parameters.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        String name = parameters.getLocalPatternName();
        if (RouteHelper.hasRoutesForPathWithPlaceholderMatch(project, name)) {
            processor.process(name);
        }
    }

    @Override
    public void processNames(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter filter) {
    }

    @Override
    public void processElementsWithName(@NotNull String name, @NotNull Processor<? super NavigationItem> processor, @NotNull FindSymbolParameters parameters) {
        Project project = parameters.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (Pair<Route, PsiElement> entry : RouteHelper.getMethodsForPathWithPlaceholderMatchRoutes(project, name)) {
            Route route = entry.getFirst();

            String path = route.getPath();
            if (path == null) {
                continue;
            }

            processor.process((NavigationItemPresentableOverwrite.create(
                entry.getSecond(),
                path,
                Symfony2Icons.ROUTE,
                "Symfony Route",
                true,
                name
            )));
        }
    }
}
