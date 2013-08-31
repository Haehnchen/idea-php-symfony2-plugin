package fr.adrienbrault.idea.symfony2plugin.templating.annotation;

import com.intellij.codeInsight.completion.*;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.annotation.AnnotationElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateLookupElement;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.util.completion.annotations.AnnotationQuoteInsertHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationCompletionContributor extends CompletionContributor {
    public AnnotationCompletionContributor() {

        extend(
                CompletionType.BASIC,
                AnnotationElementPatternHelper.getTextIdentifier("@Template"),
                new CompletionProvider<CompletionParameters>() {
                    public void addCompletions(@NotNull CompletionParameters parameters,
                                               ProcessingContext context,
                                               @NotNull CompletionResultSet resultSet) {

                        if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                            return;
                        }

                        Map<String, PsiFile> twigFilesByName = TwigHelper.getTemplateFilesByName(parameters.getPosition().getProject());
                        for (Map.Entry<String, PsiFile> entry : twigFilesByName.entrySet()) {
                            resultSet.addElement(
                                new TemplateLookupElement(entry.getKey(), entry.getValue(), parameters.getPosition() , AnnotationQuoteInsertHandler.getInstance())
                            );
                        }

                    }
                }
        );

    }
}

