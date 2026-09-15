package fr.adrienbrault.idea.symfony2plugin.doctrine

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil

private const val OBJECT_MANAGER_FIND_TRIM_KEY = '\u0183'

/**
 * \Doctrine\Common\Persistence\ObjectManager::find('REPOSITORY', $foo)
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ObjectManagerFindTypeProvider : PhpTypeProvider4 {
    override fun getKey(): Char = '\u0153'

    // Index-safe only: no PhpIndex here.
    override fun getType(e: PsiElement): PhpType? {
        val methodReference = e as? MethodReference ?: return null

        val settings = Settings.getInstance(e.project)
        if (!settings.pluginEnabled || !settings.featureTypeProvider) {
            return null
        }

        if (!PhpElementsUtil.isMethodWithFirstStringOrFieldReference(e, "find")) {
            return null
        }

        val refSignature = methodReference.signature
        if (StringUtil.isEmpty(refSignature)) {
            return null
        }

        // we need the param key on getBySignature(), since we are already in the resolved method there attach it to signature
        // param can have dotted values split with \
        if (methodReference.parameters.size >= 2) {
            val signature = PhpTypeProviderUtil.getReferenceSignatureByFirstParameter(methodReference, OBJECT_MANAGER_FIND_TRIM_KEY)
            if (signature != null) {
                return PhpType().add("#${key}$signature")
            }
        }

        return null
    }

    override fun complete(s: String, project: Project): PhpType? = null

    override fun getBySignature(
        expression: String,
        visited: Set<String>,
        depth: Int,
        project: Project,
    ): Collection<PhpNamedElement> {
        // get back our original call
        val endIndex = expression.lastIndexOf(OBJECT_MANAGER_FIND_TRIM_KEY)
        if (endIndex == -1) {
            return emptySet()
        }

        val originalSignature = expression.substring(0, endIndex)
        var parameter = expression.substring(endIndex + 1)

        // search for called method
        val phpIndex = PhpIndex.getInstance(project)
        val phpNamedElements = PhpTypeProviderUtil.getTypeSignature(phpIndex, originalSignature)
        if (phpNamedElements.isEmpty()) {
            return emptySet()
        }

        val phpNamedElement = phpNamedElements.first()
        if (phpNamedElement !is Method) {
            return emptySet()
        }

        if (
            !PhpElementsUtil.isMethodInstanceOf(phpNamedElement, "\\Doctrine\\Common\\Persistence\\ObjectManager", "find") &&
            !PhpElementsUtil.isMethodInstanceOf(phpNamedElement, "\\Doctrine\\Persistence\\ObjectManager", "find")
        ) {
            return emptySet()
        }

        parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, parameter) ?: return emptySet()

        val phpClass = EntityHelper.resolveShortcutName(project, parameter) ?: return emptySet()
        return PhpTypeProviderUtil.mergeSignatureResults(phpNamedElements, phpClass)
    }
}
