package fr.adrienbrault.idea.symfony2plugin.action.generator

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ServiceArgumentGenerateAction : CodeInsightAction() {
    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile) =
        file.fileType === XmlFileType.INSTANCE &&
            Symfony2ProjectComponent.isEnabled(project) &&
            getMatchXmlTag(editor, file) != null

    override fun getHandler() = object : CodeInsightActionHandler {
            override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
                val serviceTag = getMatchXmlTag(editor, psiFile) ?: return

                if (!ServiceActionUtil.isValidXmlParameterInspectionService(serviceTag)) {
                    IdeHelper.showErrorHintIfAvailable(editor, "Sry, not supported service definition")
                    return
                }

                val args = ServiceActionUtil.getXmlMissingArgumentTypes(
                    serviceTag,
                    true,
                    ContainerCollectionResolver.LazyServiceCollector(project)
                )
                if (args.isEmpty()) {
                    return
                }

                ServiceActionUtil.fixServiceArgument(args, serviceTag)
            }

            override fun startInWriteAction() = false
        }
}

private fun getMatchXmlTag(editor: Editor, file: PsiFile): XmlTag? {
    val xmlFile = file as? XmlFile ?: return null
    val offset = editor.caretModel.offset.takeIf { it > 0 } ?: return null
    val psiElement = xmlFile.findElementAt(offset) ?: return null

    return ServiceActionUtil.getServiceTagValid(psiElement)
}
