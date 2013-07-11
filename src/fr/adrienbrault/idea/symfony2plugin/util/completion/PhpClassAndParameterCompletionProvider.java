package fr.adrienbrault.idea.symfony2plugin.util.completion;


import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpClassIndex;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterLookupElement;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.ParameterPercentWrapInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceStringLookupElement;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PhpClassAndParameterCompletionProvider extends CompletionProvider<CompletionParameters> {

    public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {

        PsiElement psiElement = parameters.getOriginalPosition();
        if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
            return;
        }

        PhpIndex phpIndex = PhpIndex.getInstance(psiElement.getProject());

        for (String className : phpIndex.getAllClassNames(resultSet.getPrefixMatcher())) {
            resultSet.addElement(new PhpLookupElement(className, PhpClassIndex.KEY, parameters.getOriginalFile().getProject(), PhpClassReferenceInsertHandler.getInstance()));
        }

        // ParameterPercentWrapInsertHandler
        // @TODO: we need a parameter class dict
        Map<String, String> configParameters = ServiceXmlParserFactory.getInstance(parameters.getOriginalFile().getProject(), ParameterServiceParser.class).getParameterMap();
        for(Map.Entry<String, String> Entry: configParameters.entrySet()) {
            // some better class filter
            if(Entry.getValue().contains("\\") && resultSet.getPrefixMatcher().prefixMatches(Entry.getValue())) {
                resultSet.addElement(new ParameterLookupElement(Entry.getKey(), Entry.getValue(), ParameterPercentWrapInsertHandler.getInstance(), psiElement.getText()));
            }
        }

        for( Map.Entry<String, String> entry: YamlHelper.getLocalParameterMap(psiElement).entrySet()) {
            if(!configParameters.containsKey(entry.getKey())) {
                resultSet.addElement(
                    new ParameterLookupElement(entry.getKey(), entry.getValue(), ParameterPercentWrapInsertHandler.getInstance(), psiElement.getText())
                );
            }
        }

    }

}
