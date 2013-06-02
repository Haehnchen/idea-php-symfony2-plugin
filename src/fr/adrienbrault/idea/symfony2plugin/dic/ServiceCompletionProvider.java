package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ServiceCompletionProvider extends CompletionProvider<CompletionParameters> {

    public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {

        PsiElement element = parameters.getOriginalPosition();

        if(element == null) {
            return;
        }

        Symfony2ProjectComponent symfony2ProjectComponent = element.getProject().getComponent(Symfony2ProjectComponent.class);
        Map<String,String> map = symfony2ProjectComponent.getServicesMap().getMap();

        for( Map.Entry<String, String> entry: map.entrySet() ) {
            resultSet.addElement(
                new ServiceStringLookupElement(entry.getKey(), entry.getValue())
            );
        }

    }
}
