package fr.adrienbrault.idea.symfony2plugin.action.generator

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.codeInsight.intention.FileModifier
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.psi.xml.XmlFile
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.action.ui.SymfonyCreateService
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ServiceGenerateAction : CodeInsightAction() {
    override fun update(event: AnActionEvent) {
        if (!Symfony2ProjectComponent.isEnabled(event.project)) {
            event.presentation.isEnabled = false
            return
        }

        // let main implementation decide on file scope if action is enabled
        super.update(event)
    }

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile) =
        isValidForPhpClass(editor, file) || isValidForFile(file)

    override fun getHandler() = object : CodeInsightActionHandler {
            override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
                if (invokePhpClass(project, editor)) {
                    return
                }

                if (isValidForFile(psiFile)) {
                    invokeFile(project, editor)
                }
            }

            override fun getFileModifierForPreview(target: PsiFile): FileModifier? = null

            override fun startInWriteAction() = false
        }

    private fun isValidForPhpClass(editor: Editor, file: PsiFile): Boolean {
        if (file !is PhpFile) {
            return false
        }

        val offset = editor.caretModel.offset.takeIf { it > 0 } ?: return false
        val psiElement = file.findElementAt(offset) ?: return false

        return PlatformPatterns.psiElement().inside(PhpClass::class.java).accepts(psiElement)
    }

    private fun isValidForFile(file: PsiFile) = when (file) {
        is XmlFile -> file.rootTag?.name == "container"
        is YAMLFile ->
            YAMLUtil.getQualifiedKeyInFile(file, "parameters") != null ||
                YAMLUtil.getQualifiedKeyInFile(file, "services") != null
        else -> false
    }

    private fun invokeFile(project: Project, editor: Editor): Boolean {
        val file = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return false

        SymfonyCreateService.create(editor.component, project, file, editor)

        return true
    }

    private fun invokePhpClass(project: Project, editor: Editor): Boolean {
        val file = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return false

        val offset = editor.caretModel.offset.takeIf { it > 0 } ?: return false
        val psiElement = file.findElementAt(offset) ?: return false
        val phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass::class.java) ?: return false

        invokeServiceGenerator(project, file, phpClass, editor)

        return true
    }
}

fun invokeServiceGenerator(project: Project, file: PsiFile, phpClass: PhpClass, editor: Editor?) {
    editor?.let {
        SymfonyCreateService.create(it.component, project, file, phpClass, it)
    }
}
