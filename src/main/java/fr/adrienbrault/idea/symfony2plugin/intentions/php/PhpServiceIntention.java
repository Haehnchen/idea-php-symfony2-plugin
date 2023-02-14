package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.generator.ServiceGenerateAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpServiceIntention extends PsiElementBaseIntentionAction {
    @Override
    public @Nullable FileModifier getFileModifierForPreview(@NotNull PsiFile target) {
        return null;
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {

        PsiElement parentByCondition = PhpPsiUtil.getParentByCondition(psiElement, Method.INSTANCEOF);
        if(parentByCondition == null) {
            return;
        }

        PhpClass phpClass = PhpPsiUtil.getParentByCondition(psiElement, PhpClass.INSTANCEOF);
        if(phpClass == null) {
            return;
        }

        ServiceGenerateAction.invokeServiceGenerator(project, phpClass.getContainingFile(), phpClass, editor);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        PsiElement parentByCondition = PhpPsiUtil.getParentByCondition(psiElement, Method.INSTANCEOF);
        if(parentByCondition == null) {
            return false;
        }

        PhpClass phpClass = PhpPsiUtil.getParentByCondition(psiElement, PhpClass.INSTANCEOF);
        if(phpClass == null) {
            return false;
        }

        return true;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony2MethodCreateService";
    }

    @NotNull
    @Override
    public String getText() {
        return "Generate Symfony service";
    }
}
