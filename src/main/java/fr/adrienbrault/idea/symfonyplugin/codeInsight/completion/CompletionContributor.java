package fr.adrienbrault.idea.symfonyplugin.codeInsight.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.GotoCompletionProviderInterface;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.GotoCompletionProviderInterfaceEx;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.GotoCompletionProviderLookupArguments;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.utils.GotoCompletionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CompletionContributor extends com.intellij.codeInsight.completion.CompletionContributor {

    public CompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                GotoCompletionProviderLookupArguments arguments = null;
                Collection<GotoCompletionContributor> contributors = GotoCompletionUtil.getContributors(psiElement);
                for(GotoCompletionContributor contributor: contributors) {
                    GotoCompletionProviderInterface formReferenceCompletionContributor = contributor.getProvider(psiElement);
                    if(formReferenceCompletionContributor != null) {
                        completionResultSet.addAllElements(
                            formReferenceCompletionContributor.getLookupElements()
                        );
                    }

                    // extension to provide full argument pipes
                    if(formReferenceCompletionContributor instanceof GotoCompletionProviderInterfaceEx) {
                        if(arguments == null) {
                            arguments = new GotoCompletionProviderLookupArguments(completionParameters, processingContext, completionResultSet);
                        }

                        ((GotoCompletionProviderInterfaceEx) formReferenceCompletionContributor).getLookupElements(arguments);
                    }
                }
            }
        });
    }
}
