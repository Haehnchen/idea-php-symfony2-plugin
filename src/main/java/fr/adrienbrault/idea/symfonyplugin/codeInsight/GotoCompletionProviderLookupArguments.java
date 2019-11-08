package fr.adrienbrault.idea.symfonyplugin.codeInsight;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class GotoCompletionProviderLookupArguments {
    @NotNull
    private final CompletionParameters parameters;

    @NotNull
    private final ProcessingContext context;

    @NotNull
    private final CompletionResultSet resultSet;

    public GotoCompletionProviderLookupArguments(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet) {
        this.parameters = parameters;
        this.context = context;
        this.resultSet = resultSet;
    }

    @NotNull
    public CompletionParameters getParameters() {
        return parameters;
    }

    @NotNull
    public ProcessingContext getContext() {
        return context;
    }

    @NotNull
    public CompletionResultSet getResultSet() {
        return resultSet;
    }

    public void addAllElements(@NotNull Iterable<? extends LookupElement> elements) {
        resultSet.addAllElements(elements);
    }
}
