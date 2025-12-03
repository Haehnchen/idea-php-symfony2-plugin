package fr.adrienbrault.idea.symfony2plugin.form.intention;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormStringToClassConstantIntention extends PsiElementBaseIntentionAction {

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        PsiElement parent = psiElement.getParent();
        if(!(parent instanceof StringLiteralExpression)) {
            return;
        }

        try {
            FormUtil.replaceFormStringAliasWithClassConstant((StringLiteralExpression) parent);
        } catch (Exception e) {
            IdeHelper.showErrorHintIfAvailable(editor, e.getMessage());
        }
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if(!Symfony2ProjectComponent.isEnabled(psiElement.getProject())) {
            return false;
        }

        PsiElement parent = psiElement.getParent();
        if(!(parent instanceof StringLiteralExpression)) {
            return false;
        }

        String contents = ((StringLiteralExpression) parent).getContents();
        if(StringUtils.isBlank(contents)) {
            return false;
        }

        return null != new MethodMatcher.StringParameterMatcher(parent, 1)
            .withSignature(FormUtil.PHP_FORM_BUILDER_SIGNATURES)
            .match();
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony: use FormType class constant";
    }

    @NotNull
    @Override
    public String getText() {
        return getFamilyName();
    }

}
