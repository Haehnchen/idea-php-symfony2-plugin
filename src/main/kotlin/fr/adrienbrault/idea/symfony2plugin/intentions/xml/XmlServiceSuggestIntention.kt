package fr.adrienbrault.idea.symfony2plugin.intentions.xml

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.util.containers.ContainerUtil
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper
import fr.adrienbrault.idea.symfony2plugin.intentions.ui.ServiceSuggestDialog
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil
import org.apache.commons.lang3.StringUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class XmlServiceSuggestIntention : PsiElementBaseIntentionAction() {
    override fun isAvailable(project: Project, editor: Editor?, psiElement: PsiElement): Boolean {
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false
        }

        val argumentTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag::class.java)
        if (argumentTag == null || "argument" != argumentTag.name) {
            return false
        }

        return ServiceActionUtil.getServiceTagValid(psiElement) != null
    }

    override fun invoke(project: Project, editor: Editor?, psiElement: PsiElement) {
        val xmlTag = ServiceActionUtil.getServiceTagValid(psiElement)
            ?: return

        val aClass = XmlHelper.getClassFromServiceDefinition(xmlTag)
        if (aClass == null || StringUtils.isBlank(aClass)) {
            return
        }

        val argumentTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag::class.java)
            ?: return

        val argumentIndex = XmlHelper.getArgumentIndex(argumentTag)

        val suggestions = ServiceUtil.getServiceSuggestionsForServiceConstructorIndex(project, aClass, argumentIndex)
        if (suggestions.isEmpty()) {
            IdeHelper.showErrorHintIfAvailable(editor!!, "No suggestion found")
            return
        }

        ServiceSuggestDialog.create(editor!!, suggestions, MyInsertCallback(argumentTag))
    }

    override fun getFamilyName(): String {
        return "Symfony: Suggest Service"
    }

    override fun getText(): String {
        return familyName
    }

    open class MyInsertCallback(private val argumentTag: XmlTag) : ServiceSuggestDialog.Callback {
        override fun insert(selected: String) {
            // set type="service" for lazy devs
            if (ContainerUtil.find(argumentTag.attributes) { xmlAttribute -> "type" == xmlAttribute.name } == null) {
                argumentTag.setAttribute("type", "service")
            }

            // append type="SERVICE"
            argumentTag.setAttribute("id", selected)
        }
    }
}
