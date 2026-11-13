package fr.adrienbrault.idea.symfony2plugin.translation.icu.lang

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlText
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.PhpReturn
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Injects ICU MessageFormat into the messages of "+intl-icu" translation files: YAML values,
 * values of the array returned by a PHP file, and XLIFF sources and targets.
 */
class IcuMessageLanguageInjector : MultiHostInjector {
    override fun elementsToInjectIn(): List<Class<out PsiElement>> =
        listOf(YAMLScalar::class.java, StringLiteralExpression::class.java, XmlText::class.java)

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val host = context as? PsiLanguageInjectionHost ?: return
        // the file name rules out almost every string of the project, so it is checked first
        if (!TranslationUtil.isIntlIcuFile(host.containingFile.originalFile.virtualFile) || !isMessage(host) || !host.isValidHost || !Symfony2ProjectComponent.isEnabled(host)) {
            return
        }

        registrar.startInjecting(IcuMessageLanguage)
            .addPlace(null, null, host, ElementManipulators.getValueTextRange(host))
            .doneInjecting()
    }

    private fun isMessage(host: PsiLanguageInjectionHost): Boolean = when (host) {
        is YAMLScalar -> (host.parent as? YAMLKeyValue)?.value == host
        is StringLiteralExpression -> PsiTreeUtil.getParentOfType(host, ArrayHashElement::class.java)?.value == host && PsiTreeUtil.getParentOfType(host, PhpReturn::class.java) != null
        is XmlText -> host.parentTag?.name in MESSAGE_TAGS && host.parentTag?.parentTag?.name in UNIT_TAGS && TranslationUtil.isSupportedXlfFile(host.containingFile)
        else -> false
    }
}

private val MESSAGE_TAGS = setOf("source", "target")
private val UNIT_TAGS = setOf("trans-unit", "segment")
