package fr.adrienbrault.idea.symfony2plugin.translation.icu.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.FileViewProvider
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import javax.swing.Icon

/**
 * ICU MessageFormat, injected into the messages of "+intl-icu" translation files.
 */
@Suppress("JavaIoSerializableObjectMustHaveReadResolve")
object IcuMessageLanguage : Language("SymfonyIcuMessage") {
    override fun getDisplayName(): String = "ICU Message"
}

object IcuMessageFileType : LanguageFileType(IcuMessageLanguage) {
    override fun getName(): String = "Symfony ICU Message"

    override fun getDescription(): String = "ICU MessageFormat translation message"

    override fun getDefaultExtension(): String = ""

    override fun getIcon(): Icon = Symfony2Icons.TRANSLATION
}

class IcuMessageFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, IcuMessageLanguage) {
    override fun getFileType(): LanguageFileType = IcuMessageFileType

    override fun toString(): String = "ICU message"
}
