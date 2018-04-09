package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpBundleFileFactory;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpBundleCompilerPassIntention extends PsiElementBaseIntentionAction {

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {

        if(!(psiElement.getContainingFile() instanceof PhpFile)) {
            return;
        }

        PhpClass phpClass = PhpBundleFileFactory.getPhpClassForCreateCompilerScope(PsiTreeUtil.getParentOfType(psiElement, PhpClass.class));
        if(phpClass == null) {
            return;
        }

        PhpBundleFileFactory.invokeCreateCompilerPass(phpClass, editor);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        if(!(psiElement.getContainingFile() instanceof PhpFile)) {
            return false;
        }

        return PhpBundleFileFactory.getPhpClassForCreateCompilerScope(PsiTreeUtil.getParentOfType(psiElement, PhpClass.class)) != null;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony";
    }

    @NotNull
    @Override
    public String getText() {
        return "Symfony: Create CompilerPass";
    }

}
