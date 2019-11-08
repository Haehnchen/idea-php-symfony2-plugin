package fr.adrienbrault.idea.symfonyplugin.util.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.config.EventDispatcherSubscriberUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EventCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, final @NotNull CompletionResultSet completionResultSet) {

        if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
            return;
        }

        completionResultSet.addAllElements(
            EventDispatcherSubscriberUtil.getEventNameLookupElements(completionParameters.getPosition().getProject())
        );
    }
}