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
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil

private val GET_REPOSITORIES_SIGNATURES = arrayOf(
    MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ManagerRegistry", "getRepository"),
    MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectManager", "getRepository"),
    MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ManagerRegistry", "getRepository"),
    MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ObjectManager", "getRepository"),
)

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ObjectRepositoryTypeProvider : PhpTypeProvider4 {
    override fun getKey(): Char = '\u0151'

    // Index-safe only: no PhpIndex here.
    override fun getType(e: PsiElement): PhpType? {
        val methodReference = e as? MethodReference ?: return null

        val settings = Settings.getInstance(e.project)
        if (!settings.pluginEnabled || !settings.featureTypeProvider) {
            return null
        }

        if (!PhpElementsUtil.isMethodWithFirstStringOrFieldReference(e, "getRepository")) {
            return null
        }

        val refSignature = methodReference.signature
        if (StringUtil.isEmpty(refSignature)) {
            return null
        }

        val signature = PhpTypeProviderUtil.getReferenceSignatureByFirstParameter(methodReference, TRIM_KEY)
        return signature?.let { PhpType().add("#${key}$it") }
    }

    override fun complete(s: String, project: Project): PhpType? = null

    override fun getBySignature(
        expression: String,
        visited: Set<String>,
        depth: Int,
        project: Project,
    ): Collection<PhpNamedElement> {
        // get back our original call
        val endIndex = expression.lastIndexOf(TRIM_KEY)
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
            return phpNamedElements
        }

        if (!PhpElementsUtil.isMethodInstanceOf(phpNamedElement, *GET_REPOSITORIES_SIGNATURES)) {
            return phpNamedElements
        }

        // we can also pipe php references signatures and resolve them here
        // overwrite parameter to get string value
        parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, parameter) ?: return phpNamedElements

        val phpClass = EntityHelper.getEntityRepositoryClass(project, parameter)
        if (phpClass == null) {
            // self add :)
            return phpNamedElements
        }

        return PhpTypeProviderUtil.mergeSignatureResults(phpNamedElements, phpClass)
    }

    companion object {
        const val TRIM_KEY: Char = '\u0185'
    }
}
