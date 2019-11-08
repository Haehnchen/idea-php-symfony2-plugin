package fr.adrienbrault.idea.symfonyplugin.util.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.config.component.ParameterLookupElement;
import fr.adrienbrault.idea.symfonyplugin.config.yaml.ParameterPercentWrapInsertHandler;
import fr.adrienbrault.idea.symfonyplugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfonyplugin.stubs.ContainerCollectionResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpClassAndParameterCompletionProvider extends CompletionProvider<CompletionParameters> {

    public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, final @NotNull CompletionResultSet resultSet) {

        PsiElement psiElement = parameters.getOriginalPosition();
        if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
            return;
        }

        PhpClassCompletionProvider.addClassCompletion(parameters, resultSet, psiElement, false);

        for( Map.Entry<String, ContainerParameter> entry: ContainerCollectionResolver.getParameters(psiElement.getProject()).entrySet()) {
            resultSet.addElement(
                new ParameterLookupElement(entry.getValue(), ParameterPercentWrapInsertHandler.getInstance(), psiElement.getText())
            );
        }

    }

}
