package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.completion.*;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TwigRouteCompletionContributor extends CompletionContributor {

    public TwigRouteCompletionContributor() {
        extend(
            CompletionType.BASIC,
            TwigHelper.getAutocompletableRoutePattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {
                    Symfony2ProjectComponent symfony2ProjectComponent = parameters.getPosition().getProject().getComponent(Symfony2ProjectComponent.class);
                    Map<String,Route> routes = symfony2ProjectComponent.getRoutes();

                    for (Route route : routes.values()) {
                        resultSet.addElement(new RouteLookupElement(route));
                    }
                }
            }
        );
    }

}
