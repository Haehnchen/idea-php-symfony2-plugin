package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpTemplateGlobalStringGoToDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        if(!(psiElement.getContainingFile() instanceof PhpFile) || !(psiElement.getContext() instanceof StringLiteralExpression)) {
            return new PsiElement[0];
        }

        if(!Symfony2ProjectComponent.isEnabled(psiElement) || !PlatformPatterns.or(
            PlatformPatterns
                .psiElement(StringLiteralExpression.class)
                .withText(PlatformPatterns.or(
                    PlatformPatterns.string().endsWith("twig'"),
                    PlatformPatterns.string().endsWith("twig\"")
                ))
                .withLanguage(PhpLanguage.INSTANCE),
            PlatformPatterns
                .psiElement(StringLiteralExpression.class)
                .withText(PlatformPatterns.or(
                    PlatformPatterns.string().endsWith("twig'"),
                    PlatformPatterns.string().endsWith("twig\"")
                ))
                .withLanguage(PhpLanguage.INSTANCE)
            ).accepts(psiElement.getContext())) {

            return new PsiElement[0];
        }

        String templateName = PsiElementUtils.getText(psiElement);
        if(StringUtils.isBlank(templateName)) {
            return new PsiElement[0];
        }

        return TwigHelper.getTemplatePsiElements(psiElement.getProject(), templateName);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }

}
