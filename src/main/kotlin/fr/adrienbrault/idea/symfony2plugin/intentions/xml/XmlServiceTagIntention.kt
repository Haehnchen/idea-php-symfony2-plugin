package fr.adrienbrault.idea.symfony2plugin.intentions.xml

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.XmlElementFactory
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceTag
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class XmlServiceTagIntention : PsiElementBaseIntentionAction() {
    override fun isAvailable(project: Project, editor: Editor?, psiElement: PsiElement): Boolean {
        if (psiElement.containingFile.fileType !== XmlFileType.INSTANCE || !Symfony2ProjectComponent.isEnabled(project)) {
            return false
        }

        return ServiceActionUtil.getServiceTagValid(psiElement) != null
    }

    override fun invoke(project: Project, editor: Editor?, psiElement: PsiElement) {
        val xmlTag = ServiceActionUtil.getServiceTagValid(psiElement)
            ?: return

        val phpClassFromXmlTag = ServiceActionUtil.getPhpClassFromXmlTag(xmlTag, ContainerCollectionResolver.LazyServiceCollector(project))
            ?: return

        val phpServiceTags = ServiceUtil.getPhpClassServiceTags(phpClassFromXmlTag)
        if (phpServiceTags.isEmpty()) {
            IdeHelper.showErrorHintIfAvailable(editor!!, "Ops, no possible Tag found")
            return
        }

        for (tag in xmlTag.subTags) {
            if ("tag" != tag.name) {
                continue
            }

            val name = tag.getAttribute("name")
                ?: continue

            val value = name.value
            phpServiceTags.remove(value)
        }

        ServiceUtil.insertTagWithPopupDecision(editor!!, phpServiceTags) { tag ->
            val serviceTag = ServiceTag(phpClassFromXmlTag, tag)
            ServiceUtil.decorateServiceTag(serviceTag)
            xmlTag.addSubTag(XmlElementFactory.getInstance(project).createTagFromText(serviceTag.toXmlString()), false)
        }
    }


    override fun getFamilyName(): String {
        return "Symfony: Add Tags"
    }

    override fun getText(): String {
        return familyName
    }
}
