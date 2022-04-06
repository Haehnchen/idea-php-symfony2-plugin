package de.espend.idea.php.drupal.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import de.espend.idea.php.drupal.DrupalProjectComponent;
import de.espend.idea.php.drupal.utils.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpCompletionContributor extends CompletionContributor {

    public PhpCompletionContributor() {

        // t('foo');
        // @TODO: pattern
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if(psiElement == null || !DrupalProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                PsiElement literal = psiElement.getContext();
                if (!(literal instanceof StringLiteralExpression)) {
                    return;
                }

                PsiElement parameterList = literal.getParent();
                if (!(parameterList instanceof ParameterList)) {
                    return;
                }

                PsiElement functionReference = parameterList.getParent();
                if (!(functionReference instanceof FunctionReference) || !"t".equals(((FunctionReference) functionReference).getName())) {
                    return;
                }

                TranslationUtil.attachGetTextLookupKeys(completionResultSet, psiElement.getProject());

            }

        });

        // 'route_name' => 'foo';
        // @TODO: pattern
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if(psiElement == null || !DrupalProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                PsiElement arrayValueString = psiElement.getContext();
                if (arrayValueString instanceof StringLiteralExpression) {
                    PsiElement arrayValue = arrayValueString.getParent();
                    if(arrayValue != null && arrayValue.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {
                        PsiElement arrayHashElement = arrayValue.getParent();
                        if(arrayHashElement instanceof ArrayHashElement) {
                            PhpPsiElement arrayKey = ((ArrayHashElement) arrayHashElement).getKey();
                            if(arrayKey instanceof StringLiteralExpression && "route_name".equals(((StringLiteralExpression) arrayKey).getContents())) {
                                completionResultSet.addAllElements(RouteHelper.getRoutesLookupElements(psiElement.getProject()));
                            }
                        }
                    }
                }

            }

        });

    }
}
