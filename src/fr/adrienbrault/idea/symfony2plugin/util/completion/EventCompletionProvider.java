package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlEventParser;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class EventCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

        if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
            return;
        }

        XmlEventParser xmlEventParser = ServiceXmlParserFactory.getInstance(completionParameters.getPosition().getProject(), XmlEventParser.class);
        for(Map.Entry<String, String> event : xmlEventParser.get().entrySet()) {
            completionResultSet.addElement(LookupElementBuilder.create(event.getKey()).withTypeText(event.getValue(), true));
        }

    }
}