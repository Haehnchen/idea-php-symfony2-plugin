package fr.adrienbrault.idea.symfony2plugin.dic.intention

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class PhpServiceArgumentIntention : PsiElementBaseIntentionAction() {
    override fun invoke(project: Project, editor: Editor?, psiElement: PsiElement) {
        val phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass::class.java)
            ?: return

        val serviceNames = ContainerCollectionResolver.ServiceCollector.create(project)
            .convertClassNameToServices(phpClass.fqn)
        if (serviceNames.isEmpty()) {
            return
        }

        val psiElements = ArrayList<PsiElement>()
        for (serviceName in serviceNames) {
            psiElements.addAll(ServiceIndexUtil.findServiceDefinitions(project, serviceName))
        }

        if (psiElements.isEmpty()) {
            return
        }

        val map = HashMap<String?, PsiElement>()

        for (element in psiElements) {
            map[VfsUtil.getRelativePath(element.containingFile.virtualFile, ProjectUtil.getProjectDir(element))] = element
        }

        JBPopupFactory.getInstance().createPopupChooserBuilder(ArrayList(map.keys))
            .setTitle("Symfony: Services Definitions")
            .setItemChosenCallback { selectedValue ->
                WriteCommandAction.writeCommandAction(project)
                    .withName("Service Update")
                    .run<RuntimeException> { invokeByScope(project, map[selectedValue]!!, editor!!) }
            }
            .createPopup()
            .showInBestPositionFor(editor!!)
    }

    private fun invokeByScope(project: Project, psiElement: PsiElement, editor: Editor) {
        var success = false

        if (psiElement is XmlTag) {
            val args = ServiceActionUtil.getXmlMissingArgumentTypes(
                psiElement,
                true,
                ContainerCollectionResolver.LazyServiceCollector(psiElement.project),
            )

            success = args.isNotEmpty()
            if (success) {
                ServiceActionUtil.fixServiceArgument(args, psiElement)
            }
        } else if (psiElement is YAMLKeyValue) {
            val args = ServiceActionUtil.getYamlMissingArgumentTypes(
                project,
                ServiceActionUtil.ServiceYamlContainer.create(psiElement),
                false,
                ContainerCollectionResolver.LazyServiceCollector(psiElement.project),
            )

            success = args.isNotEmpty()
            if (success) {
                ServiceActionUtil.fixServiceArgument(psiElement)
            }
        }

        if (!success) {
            IdeHelper.showErrorHintIfAvailable(editor, "No argument update needed")
            return
        }

        var relativePath = VfsUtil.getRelativePath(
            psiElement.containingFile.virtualFile,
            ProjectUtil.getProjectDir(psiElement),
        )
        if (relativePath == null) {
            relativePath = "n/a"
        }

        HintManager.getInstance().showInformationHint(editor, String.format("Argument updated: %s", relativePath))
    }

    override fun isAvailable(project: Project, editor: Editor?, psiElement: PsiElement): Boolean {
        return psiElement.language == PhpLanguage.INSTANCE && getServicesInScope(project, psiElement).isNotEmpty()
    }

    override fun getFamilyName(): String {
        return "Symfony: Update service arguments"
    }

    override fun getText(): String {
        return familyName
    }

    private fun getServicesInScope(project: Project, psiElement: PsiElement): Set<String> {
        val phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass::class.java)

        return if (phpClass == null) {
            emptySet()
        } else {
            ContainerCollectionResolver.ServiceCollector.create(project).convertClassNameToServices(phpClass.fqn)
        }
    }

    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        return IntentionPreviewInfo.EMPTY
    }
}
