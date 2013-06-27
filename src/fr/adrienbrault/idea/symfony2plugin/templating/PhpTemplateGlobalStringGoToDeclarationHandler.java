package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpTemplateGlobalStringGoToDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement) || !PlatformPatterns.or(
            PlatformPatterns
                .psiElement(PhpTokenTypes.STRING_LITERAL)
                .withText(PlatformPatterns.or(
                    PlatformPatterns.string().endsWith("twig'"),
                    PlatformPatterns.string().endsWith("twig\"")
                ))
                .withLanguage(PhpLanguage.INSTANCE),
            PlatformPatterns
                .psiElement(PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE)
                .withText(PlatformPatterns.or(
                    PlatformPatterns.string().endsWith("twig'"),
                    PlatformPatterns.string().endsWith("twig\"")
                ))
                .withLanguage(PhpLanguage.INSTANCE)
            ).accepts(psiElement)) {

            return new PsiElement[0];
        }

        String templateName = PsiElementUtils.getText(psiElement);
        return TwigHelper.getTemplatePsiElements(psiElement.getProject(), templateName);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }

}
