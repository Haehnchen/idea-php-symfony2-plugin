package fr.adrienbrault.idea.symfony2plugin.templating.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteLookupElement;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHtmlCompletionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TwigHtmlCompletionContributor extends CompletionContributor {

    public TwigHtmlCompletionContributor() {

        // add completion for href and provide twig insert handler
        // <a href="#">#</a>
        // <a href="{{ path('', {'foo' : 'bar'}) }}">#</a>
        extend(CompletionType.BASIC, TwigHtmlCompletionUtil.getHrefAttributePattern(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {

                if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                    return;
                }

                List<LookupElement> routesLookupElements = RouteHelper.getRoutesLookupElements(parameters.getPosition().getProject());
                for(LookupElement element: routesLookupElements) {
                    if(element instanceof RouteLookupElement) {
                        ((RouteLookupElement) element).withInsertHandler(TwigPathFunctionInsertHandler.getInstance());
                    }
                }

                resultSet.addAllElements(routesLookupElements);

            }
        });

    }

}
