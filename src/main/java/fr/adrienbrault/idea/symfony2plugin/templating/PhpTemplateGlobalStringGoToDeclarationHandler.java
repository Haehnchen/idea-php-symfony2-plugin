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
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpTemplateGlobalStringGoToDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {
        if(!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContainingFile() instanceof PhpFile)) {
            return null;
        }

        PsiElement stringLiteral = psiElement.getContext();
        if(!(stringLiteral instanceof StringLiteralExpression)) {
            return new PsiElement[0];
        }

        if(!PlatformPatterns.or(
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
            ).accepts(stringLiteral)) {

            return new PsiElement[0];
        }

        String templateName = ((StringLiteralExpression) stringLiteral).getContents();
        if(StringUtils.isBlank(templateName)) {
            return new PsiElement[0];
        }

        // file and directory navigation:
        // foo.html.twig, foobar/foo.html.twig
        return TwigUtil.getTemplateNavigationOnOffset(psiElement.getProject(), templateName, i - psiElement.getTextRange().getStartOffset()).toArray(new PsiElement[0]);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }

}
