package fr.adrienbrault.idea.symfony2plugin.intentions.yaml

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.translation.util.TranslationInsertUtil
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceTag
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper
import org.apache.commons.lang3.StringUtils
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLCompoundValue
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLSequence

private fun collectServiceTags(project: Project, serviceKeyValue: YAMLKeyValue): Pair<PhpClass, MutableSet<String>>? {
    var className = YamlHelper.getYamlKeyValueAsString(serviceKeyValue, "class")
    if (className == null || StringUtils.isBlank(className)) {
        val key = serviceKeyValue.key
        if (key != null) {
            val text = key.text
            if (StringUtils.isNotBlank(text) && YamlHelper.isClassServiceId(text)) {
                className = text
            }
        }
    }

    if (className == null) {
        return null
    }

    val resolvedClassDefinition = ServiceUtil.getResolvedClassDefinition(
        project,
        className,
        ContainerCollectionResolver.LazyServiceCollector(project),
    ) ?: return null

    val phpClassServiceTags = ServiceUtil.getPhpClassServiceTags(resolvedClassDefinition)

    val strings = YamlHelper.collectServiceTags(serviceKeyValue)
    if (strings.isNotEmpty()) {
        for (string in strings) {
            phpClassServiceTags.remove(string)
        }
    }

    return Pair.create(resolvedClassDefinition, phpClassServiceTags)
}

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class YamlServiceTagIntention : PsiElementBaseIntentionAction() {
    override fun isAvailable(project: Project, editor: Editor?, psiElement: PsiElement): Boolean {
        if (psiElement.containingFile.fileType !== YAMLFileType.YML || !Symfony2ProjectComponent.isEnabled(psiElement.project)) {
            return false
        }

        return YamlHelper.findServiceInContext(psiElement) != null
    }

    override fun invoke(project: Project, editor: Editor?, psiElement: PsiElement) {
        val serviceKeyValue = YamlHelper.findServiceInContext(psiElement)
            ?: return

        val invoke = collectServiceTags(project, serviceKeyValue)
            ?: return

        val phpClassServiceTags = invoke.second
        if (phpClassServiceTags.isEmpty()) {
            IdeHelper.showErrorHintIfAvailable(editor!!, "Ops, no possible Tag found")
            return
        }

        val resolvedClassDefinition = invoke.first

        var appendEndOffset = -1
        var insertString: String? = null

        val argumentsKeyValue = YamlHelper.getYamlKeyValue(serviceKeyValue, "tags")
        if (argumentsKeyValue == null) {
            // we dont an "tags" key so we need to create one

            val indent = StringUtil.repeatSymbol(' ', YAMLUtil.getIndentToThisElement(serviceKeyValue))

            val yamlSequences = ArrayList<String>()
            for (item in phpClassServiceTags) {
                val serviceTag = ServiceTag(resolvedClassDefinition, item)
                ServiceUtil.decorateServiceTag(serviceTag)
                yamlSequences.add(indent + " " + serviceTag.toYamlString())
            }

            appendEndOffset = serviceKeyValue.textRange.endOffset

            val eol = TranslationInsertUtil.findLineSeparator(serviceKeyValue)
            insertString = eol + indent + "tags:" + eol + StringUtils.join(yamlSequences, eol)
        } else {
            // we found a "tags" key so update
            val value = argumentsKeyValue.value
            if (value !is YAMLCompoundValue) {
                IdeHelper.showErrorHintIfAvailable(editor!!, "Sry, not supported tags definition")
                return
            }

            val firstChild = value.firstChild
            if (firstChild is YAMLSequence) {
                val indent = StringUtil.repeatSymbol(' ', YAMLUtil.getIndentToThisElement(argumentsKeyValue))

                val yamlSequences = ArrayList<String>()
                for (item in phpClassServiceTags) {
                    val serviceTag = ServiceTag(resolvedClassDefinition, item)
                    ServiceUtil.decorateServiceTag(serviceTag)
                    yamlSequences.add(indent + serviceTag.toYamlString())
                }

                appendEndOffset = argumentsKeyValue.textRange.endOffset

                val eol = TranslationInsertUtil.findLineSeparator(argumentsKeyValue)
                insertString = eol + StringUtils.join(yamlSequences, eol)
            }
        }

        if (appendEndOffset == -1) {
            IdeHelper.showErrorHintIfAvailable(editor!!, "Sry, not supported service definition")
            return
        }

        val manager = PsiDocumentManager.getInstance(project)
        val document = manager.getDocument(serviceKeyValue.containingFile)
            ?: return

        document.insertString(appendEndOffset, insertString!!)
        manager.doPostponedOperationsAndUnblockDocument(document)
        manager.commitDocument(document)
    }

    override fun getFamilyName(): String {
        return "Symfony: Add Tags"
    }

    override fun getText(): String {
        return familyName
    }
}
