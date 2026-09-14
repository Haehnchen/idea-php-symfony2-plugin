package fr.adrienbrault.idea.symfony2plugin.action.generator

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilBase
import com.jetbrains.php.lang.psi.PhpFile
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpBundleFileFactory

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class PhpBundleCompilerPassGenerateAction : CodeInsightAction() {
    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile) =
        file is PhpFile &&
            Symfony2ProjectComponent.isEnabled(project) &&
            PhpBundleFileFactory.getPhpClassForCreateCompilerScope(editor, file) != null

    override fun getHandler(): CodeInsightActionHandler = MyCodeInsightActionHandler()

    private class MyCodeInsightActionHandler : CodeInsightActionHandler {
        override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
            PhpBundleFileFactory.getPhpClassForCreateCompilerScope(
                editor,
                PsiUtilBase.getPsiFileInEditor(editor, project)
            )?.let { phpClass ->
                PhpBundleFileFactory.invokeCreateCompilerPass(phpClass, editor)
            }
        }
    }
}
