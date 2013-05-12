package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.completion.*;
import com.intellij.util.ProcessingContext;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TwigTemplateCompletionContributor extends CompletionContributor {

    public TwigTemplateCompletionContributor() {
        extend(
            CompletionType.BASIC,
            TwigHelper.getAutocompletableTemplatePattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {
                    Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(parameters.getPosition().getProject());
                    for (Map.Entry<String, TwigFile> entry : twigFilesByName.entrySet()) {
                        resultSet.addElement(
                            new TemplateLookupElement(entry.getKey(), entry.getValue())
                        );
                    }
                }
            }
        );
    }

}