package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.AbstractGuessTypoQuickFix;
import fr.adrienbrault.idea.symfony2plugin.util.SimilarSuggestionUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteGuessTypoQuickFix extends AbstractGuessTypoQuickFix {
    private final String missingRoute;

    public RouteGuessTypoQuickFix(@NotNull String missingRoute) {
        this.missingRoute = missingRoute;
    }

    @Override
    protected @NotNull String getSuggestionLabel() {
        return "Route";
    }

    @Override
    protected @NotNull List<String> getSimilarItems(@NotNull Project project) {
        return SimilarSuggestionUtil.findSimilarString(this.missingRoute, RouteHelper.getAllRoutes(project).keySet());
    }
}
