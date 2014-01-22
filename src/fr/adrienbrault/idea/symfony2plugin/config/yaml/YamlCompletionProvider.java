package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlCompletionProvider extends CompletionProvider<CompletionParameters> {

    private List<LookupElement> lookupElements = new ArrayList<LookupElement>();

    YamlCompletionProvider(List<LookupElement> lookups) {
        lookupElements = lookups;
    }

    YamlCompletionProvider(String[] lookups) {

        for (String lookup : lookups) {
            lookupElements.add(LookupElementBuilder.create(lookup));
        }

    }

    public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {

        if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
            return;
        }

        resultSet.addAllElements(lookupElements);
    }
}
