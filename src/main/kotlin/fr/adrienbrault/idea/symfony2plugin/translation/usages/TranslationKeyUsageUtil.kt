package fr.adrienbrault.idea.symfony2plugin.translation.usages

import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlText
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TranslationStubIndex
import fr.adrienbrault.idea.symfony2plugin.translation.collector.YamlTranslationVisitor
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil
import org.apache.commons.lang3.StringUtils
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

data class TranslationKeyDeclaration(val key: String, val domain: String, val element: PsiElement)

object TranslationKeyUsageUtil {
    @JvmStatic
    fun getDeclaration(element: PsiElement): TranslationKeyDeclaration? {
        val file = element.containingFile ?: return null
        if (!Symfony2ProjectComponent.isEnabled(element) || DumbService.isDumb(element.project)) {
            return null
        }

        return when (file) {
            is YAMLFile -> getYamlDeclaration(element)
            is PhpFile -> getPhpDeclaration(element)
            else -> if (TranslationUtil.isSupportedXlfFile(file)) getXliffDeclaration(element) else null
        }
    }

    @JvmStatic
    fun getLocationString(declaration: TranslationKeyDeclaration): String {
        val virtualFile = declaration.element.containingFile?.virtualFile ?: return declaration.domain
        val relativePath = StringUtils.defaultIfBlank(
            VfsExUtil.getRelativeProjectPathStrict(declaration.element.project, virtualFile),
            VfsExUtil.getRelativeProjectPath(declaration.element.project, virtualFile),
        )

        return if (StringUtils.isBlank(relativePath)) declaration.domain else "${declaration.domain}, $relativePath"
    }

    private fun getYamlDeclaration(element: PsiElement): TranslationKeyDeclaration? {
        val keyValue = element as? YAMLKeyValue ?: (element.parent as? YAMLKeyValue)?.takeIf { it.key == element } ?: return null
        if (keyValue.value is YAMLMapping) {
            return null
        }

        val keys = generateSequence(keyValue) { PsiTreeUtil.getParentOfType(it, YAMLKeyValue::class.java) }.toList()
            .map { YamlTranslationVisitor.keyNormalizer(it.keyText).takeUnless { key -> StringUtils.isBlank(key) } ?: return null }

        return createDeclaration(keyValue, keys.asReversed().joinToString("."))
    }

    private fun getPhpDeclaration(element: PsiElement): TranslationKeyDeclaration? {
        val literal = element as? StringLiteralExpression ?: element.parent as? StringLiteralExpression ?: return null
        val hashElement = PsiTreeUtil.getParentOfType(literal, ArrayHashElement::class.java)
        if (hashElement == null || hashElement.key != literal || hashElement.value is ArrayCreationExpression) {
            return null
        }

        val keys = generateSequence(hashElement) { PsiTreeUtil.getParentOfType(it, ArrayHashElement::class.java) }.toList()
            .map { (it.key as? StringLiteralExpression)?.contents ?: return null }

        return createDeclaration(literal, keys.asReversed().joinToString("."))
    }

    private fun getXliffDeclaration(element: PsiElement): TranslationKeyDeclaration? {
        val sourceTag = when {
            element is XmlTag -> element
            element.parent is XmlText -> element.parent.parent as? XmlTag
            else -> null
        }

        if (sourceTag != null && sourceTag.name == "source" && sourceTag.parentTag?.name in setOf("trans-unit", "segment")) {
            val key = sourceTag.value.text
            return if (StringUtils.isBlank(key)) null else createDeclaration(sourceTag, key)
        }

        val attribute = when (element) {
            is XmlAttribute -> element
            is XmlAttributeValue -> element.parent as? XmlAttribute
            else -> (element.parent as? XmlAttributeValue)?.parent as? XmlAttribute
        }

        if (attribute == null || attribute.name != "resname" || attribute.parent?.name != "trans-unit") {
            return null
        }

        val key = attribute.value?.takeUnless { StringUtils.isBlank(it) } ?: return null

        // The XLIFF index only stores <source> texts, so a resname is matched by the file domain alone.
        val domain = getIndexedDomains(attribute.containingFile).keys.singleOrNull() ?: return null
        return TranslationKeyDeclaration(key, domain, attribute)
    }

    private fun createDeclaration(element: PsiElement, key: String): TranslationKeyDeclaration? {
        val domain = getIndexedDomains(element.containingFile).entries.firstOrNull { key in it.value }?.key ?: return null
        return TranslationKeyDeclaration(key, domain, element)
    }

    private fun getIndexedDomains(file: PsiFile): Map<String, Set<String>> {
        val virtualFile = file.virtualFile ?: file.originalFile.virtualFile ?: return emptyMap()
        return FileBasedIndex.getInstance().getFileData(TranslationStubIndex.KEY, virtualFile, file.project)
    }
}
