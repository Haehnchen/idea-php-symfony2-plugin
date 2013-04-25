package fr.adrienbrault.idea.symfony2plugin.config.annotation;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationCompletionProvider extends CompletionProvider<CompletionParameters> {

    private ArrayList<LookupElement> lookupElements;

    public AnnotationCompletionProvider(ArrayList<LookupElement> lookups) {
        lookupElements = lookups;
    }

    public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
        resultSet.addAllElements(lookupElements);
    }
}
