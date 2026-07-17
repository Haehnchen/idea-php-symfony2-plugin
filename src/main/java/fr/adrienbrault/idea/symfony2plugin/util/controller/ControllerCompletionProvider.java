package fr.adrienbrault.idea.symfony2plugin.util.controller;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
        Project project = completionParameters.getPosition().getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        List<LookupElement> lookupElements = ControllerIndex.getControllerLookupElements(project);
        CompletionResultSet completionResultSet = TwigUtil.withCompletionPrefix(completionParameters, resultSet);
        completionResultSet.addAllElements(lookupElements);

    }
}
