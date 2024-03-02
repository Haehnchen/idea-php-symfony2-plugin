package fr.adrienbrault.idea.symfony2plugin.action.generator;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpBundleFileFactory;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpBundleCompilerPassGenerateAction extends CodeInsightAction {
    @Override
    protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if(!(file instanceof PhpFile) || !Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        return PhpBundleFileFactory.getPhpClassForCreateCompilerScope(editor, file) != null;
    }

    @NotNull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new MyCodeInsightActionHandler();
    }

    private static class MyCodeInsightActionHandler implements CodeInsightActionHandler {
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
