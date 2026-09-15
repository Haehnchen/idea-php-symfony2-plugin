package fr.adrienbrault.idea.symfony2plugin.dic

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil

private const val TRIM_KEY = '\u0182'

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class SymfonyContainerTypeProvider : PhpTypeProvider4 {
    override fun getKey(): Char = '\u0150'

    // Index-safe only: no PhpIndex here.
    override fun getType(e: PsiElement): PhpType? {
        // container calls are only on "get" methods
        val methodReference = e as? MethodReference ?: return null

        val settings = Settings.getInstance(e.project)
        if (!settings.pluginEnabled || !settings.featureTypeProvider) {
            return null
        }

        // container calls are only on "get" methods
        if (!PhpElementsUtil.isMethodWithFirstStringOrFieldReference(e, "get")) {
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
        val signature = PhpTypeProviderUtil.resolveSignatureSplit(expression, TRIM_KEY, project) ?: return emptySet()

        val phpNamedElement = signature.elements().first()
        if (phpNamedElement !is Method) {
            return signature.elements()
        }

        val parameter = PhpTypeProviderUtil.getResolvedParameter(signature.phpIndex(), signature.parameter())
            ?: return signature.elements()

        // finally search the classes
        if (ServiceContainerUtil.isServiceGetMethod(phpNamedElement)) {
            val containerService = ContainerCollectionResolver.getService(project, parameter)
            if (containerService != null) {
                val phpClasses = mutableSetOf<PhpNamedElement>()
                containerService.classNames.forEach {
                    phpClasses.addAll(PhpIndex.getInstance(project).getAnyByFQN(it))
                }

                return phpClasses
            }
        }

        return signature.elements()
    }
}
