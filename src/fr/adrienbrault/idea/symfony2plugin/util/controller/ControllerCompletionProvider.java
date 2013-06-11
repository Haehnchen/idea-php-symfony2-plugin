package fr.adrienbrault.idea.symfony2plugin.util.controller;


import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

public class ControllerCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

        if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
            return;
        }

        PhpIndex phpIndex = PhpIndex.getInstance(completionParameters.getPosition().getProject());

        ControllerIndex controllerIndex = new ControllerIndex(phpIndex);
        for(ControllerAction controllerAction: controllerIndex.getAction()) {
            completionResultSet.addElement(new ControllerActionLookupElement(controllerAction));
        }

        for(ControllerAction controllerAction: controllerIndex.getServiceActionMethods(completionParameters.getPosition().getProject())) {
            completionResultSet.addElement(new ControllerActionLookupElement(controllerAction));
        }

    }
}
