package fr.adrienbrault.idea.symfony2plugin.action.generator;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpBundleFileFactory;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpBundleCompilerPassGenerateAction extends CodeInsightAction {

    @Override
    public void update(AnActionEvent event) {
        super.update(event);
        boolean enabled = Symfony2ProjectComponent.isEnabled(event.getProject());
        event.getPresentation().setVisible(enabled);
        event.getPresentation().setEnabled(enabled);
    }

    @Override
    protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return PhpBundleFileFactory.getPhpClassForCreateCompilerScope(editor, file) != null;
    }

    @NotNull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new MyCodeInsightActionHandler();
    }

    private class MyCodeInsightActionHandler implements CodeInsightActionHandler {
        @Override
        public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
            PhpClass phpClass = PhpBundleFileFactory.getPhpClassForCreateCompilerScope(editor, PsiUtilBase.getPsiFileInEditor(editor, project));
            if(phpClass != null) {
                PhpBundleFileFactory.invokeCreateCompilerPass(phpClass, editor);
            }
        }

        @Override
        public boolean startInWriteAction() {
            return true;
        }
    }

}
