package de.espend.idea.php.drupal.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import de.espend.idea.php.drupal.DrupalIcons;
import de.espend.idea.php.drupal.DrupalProjectComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLDocument;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlCompletionContributor extends CompletionContributor {

    final private static String[] MODULE_KEYS = new String[] {"name", "type", "description", "package", "version", "core", "configure", "dependencies", "required"};

    public YamlCompletionContributor() {

        extend(
            CompletionType.BASIC, PlatformPatterns.psiElement().withParent(PlatformPatterns.psiElement(YAMLDocument.class)).inFile(
            PlatformPatterns.psiFile().withName(PlatformPatterns.string().endsWith(".info.yml"))
        ),
            new CompletionProvider<CompletionParameters>() {
                @Override
                protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                    if(!DrupalProjectComponent.isEnabled(completionParameters.getOriginalPosition())) {
                        return;
                    }

                    for(String key: MODULE_KEYS) {
                        completionResultSet.addElement(LookupElementBuilder.create(key).withIcon(DrupalIcons.DRUPAL));
                    }

                }
            }
        );
    }

}
