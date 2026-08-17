package fr.adrienbrault.idea.symfony2plugin.intentions.php

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class XmlServiceArgumentIntention : PsiElementBaseIntentionAction() {
    override fun invoke(project: Project, editor: Editor?, psiElement: PsiElement) {
        val xmlTag = ServiceActionUtil.getServiceTagValid(psiElement)
            ?: return

        val args = ServiceActionUtil.getXmlMissingArgumentTypes(xmlTag, true, ContainerCollectionResolver.LazyServiceCollector(project))
        if (args.isEmpty()) {
            return
        }

        ServiceActionUtil.fixServiceArgument(args, xmlTag)
    }

    override fun isAvailable(project: Project, editor: Editor?, psiElement: PsiElement): Boolean {
        if (psiElement.containingFile.fileType !== XmlFileType.INSTANCE || !Symfony2ProjectComponent.isEnabled(project)) {
            return false
        }

        val serviceTagValid = ServiceActionUtil.getServiceTagValid(psiElement)
            ?: return false

        if (!ServiceActionUtil.isValidXmlParameterInspectionService(serviceTagValid)) {
            return false
        }

        return ServiceActionUtil.getXmlMissingArgumentTypes(serviceTagValid, true, ContainerCollectionResolver.LazyServiceCollector(project)).isNotEmpty()
    }

    override fun getFamilyName(): String {
        return "Symfony: Add Arguments"
    }

    override fun getText(): String {
        return familyName
    }

    override fun startInWriteAction(): Boolean {
        return false
    }
}
