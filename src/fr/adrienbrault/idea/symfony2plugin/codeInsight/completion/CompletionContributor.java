package fr.adrienbrault.idea.symfony2plugin.codeInsight.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProviderInterface;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import org.jetbrains.annotations.NotNull;

public class CompletionContributor extends com.intellij.codeInsight.completion.CompletionContributor {

    public CompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                for(GotoCompletionContributor contributor: GotoCompletionUtil.getContributors(psiElement)) {
                    GotoCompletionProviderInterface formReferenceCompletionContributor = contributor.getProvider(psiElement);
                    if(formReferenceCompletionContributor != null) {
                        completionResultSet.addAllElements(
                            formReferenceCompletionContributor.getLookupElements()
                        );
                    }
                }

            }
        });
    }

}
