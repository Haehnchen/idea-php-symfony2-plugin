package de.espend.idea.php.drupal.utils;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import de.espend.idea.php.drupal.DrupalIcons;
import de.espend.idea.php.drupal.index.GetTextFileIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import org.jetbrains.annotations.NotNull;

public class TranslationUtil {
    public static void attachGetTextLookupKeys(@NotNull CompletionResultSet completionResultSet, @NotNull Project project) {
        for(String phpClassName: SymfonyProcessors.createResult(project, GetTextFileIndex.KEY)) {
            completionResultSet.addElement(LookupElementBuilder.create(phpClassName).withIcon(DrupalIcons.DRUPAL));
        }
    }
}
