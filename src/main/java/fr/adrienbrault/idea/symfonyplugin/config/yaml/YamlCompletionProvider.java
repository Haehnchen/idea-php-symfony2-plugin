package fr.adrienbrault.idea.symfonyplugin.config.yaml;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfonyplugin.Symfony2Icons;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Nullable
    private List<LookupElement> lookupList;

    @Nullable
    private Map<String, String> lookupMap;

    public YamlCompletionProvider(@Nullable List<LookupElement> lookups) {
        this.lookupList = lookups;
    }

    public YamlCompletionProvider(@Nullable Map<String, String> lookups) {
        this.lookupMap = lookups;
    }

    public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
        if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
            return;
        }

        if(this.lookupList != null) {
            resultSet.addAllElements(this.lookupList);
        } else if(lookupMap != null) {
            for (Map.Entry<String, String> lookup : lookupMap.entrySet()) {
                LookupElementBuilder lookupElement = LookupElementBuilder.create(lookup.getKey()).withTypeText(lookup.getValue(), true).withIcon(Symfony2Icons.SYMFONY);
                if(lookup.getValue() != null && lookup.getValue().contains("deprecated")) {
                    lookupElement = lookupElement.withStrikeoutness(true);
                }

                resultSet.addElement(lookupElement);
            }
        }
    }
}
