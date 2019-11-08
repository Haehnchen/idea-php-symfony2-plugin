package fr.adrienbrault.idea.symfonyplugin.util.controller;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

        if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
            return;
        }

        completionResultSet.addAllElements(ControllerIndex.getControllerLookupElements(completionParameters.getPosition().getProject()));

    }
}
