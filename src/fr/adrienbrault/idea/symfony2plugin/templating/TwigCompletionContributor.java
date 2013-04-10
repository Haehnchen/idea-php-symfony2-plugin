package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTagWithFileReference;
import com.jetbrains.twig.TwigTokenTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TwigCompletionContributor extends CompletionContributor {

    public TwigCompletionContributor() {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT).withParent(TwigTagWithFileReference.class).withLanguage(TwigLanguage.INSTANCE),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {
                    Map<String, TwigFile> twigFilesByName = TemplateHelper.getTwigFilesByName(parameters.getPosition().getProject());
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
