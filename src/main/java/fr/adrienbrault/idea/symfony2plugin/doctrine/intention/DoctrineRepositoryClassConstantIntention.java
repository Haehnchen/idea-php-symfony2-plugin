package fr.adrienbrault.idea.symfony2plugin.doctrine.intention;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.SymfonyPhpReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineRepositoryClassConstantIntention extends PsiElementBaseIntentionAction {

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        PsiElement parent = psiElement.getParent();
        if(!(parent instanceof StringLiteralExpression)) {
            return;
        }

        try {
            PhpClass phpClass = EntityHelper.resolveShortcutName(project, ((StringLiteralExpression) parent).getContents());
            if(phpClass == null) {
                throw new Exception("Can not resolve model class");
            }
            PhpElementsUtil.replaceElementWithClassConstant(phpClass, parent);
        } catch (Exception e) {
            HintManager.getInstance().showErrorHint(editor, e.getMessage());
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

        return null != new MethodMatcher.StringParameterMatcher(parent, 0)
            .withSignature(SymfonyPhpReferenceContributor.REPOSITORY_SIGNATURES)
            .withSignature("Doctrine\\Persistence\\ObjectManager", "find")
            .withSignature("Doctrine\\Common\\Persistence\\ObjectManager", "find") // @TODO: missing somewhere
            .match();
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony";
    }

    @NotNull
    @Override
    public String getText() {
        return "Doctrine: use class constant";
    }


}
