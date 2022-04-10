package de.espend.idea.php.drupal.completion;


import com.intellij.codeInsight.completion.*;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import de.espend.idea.php.drupal.DrupalProjectComponent;
import de.espend.idea.php.drupal.utils.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigCompletionContributor extends CompletionContributor {

    public TwigCompletionContributor() {

        // ''|t;
        extend(CompletionType.BASIC, TwigPattern.getTranslationPattern("t"), new CompletionProvider<CompletionParameters>() {

            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if(psiElement == null || !DrupalProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                TranslationUtil.attachGetTextLookupKeys(completionResultSet, psiElement.getProject());

            }

        });

    }



}
