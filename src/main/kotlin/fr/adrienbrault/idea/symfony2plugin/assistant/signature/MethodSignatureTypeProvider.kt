package fr.adrienbrault.idea.symfony2plugin.assistant.signature

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.extension.MethodSignatureTypeProviderExtension
import fr.adrienbrault.idea.symfony2plugin.extension.MethodSignatureTypeProviderParameter
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil

private const val METHOD_SIGNATURE_TRIM_KEY = '\u0181'
private val METHOD_SIGNATURE_EXTENSIONS = ExtensionPointName.create<MethodSignatureTypeProviderExtension>(
    "fr.adrienbrault.idea.symfony2plugin.extension.MethodSignatureTypeProviderExtension",
)

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class MethodSignatureTypeProvider : PhpTypeProvider4 {
    override fun getKey(): Char = '\u0160'

    // Index-safe only: no PhpIndex here.
    override fun getType(e: PsiElement): PhpType? {
        val methodReference = e as? MethodReference ?: return null

        val settings = Settings.getInstance(e.project)
        if (!settings.pluginEnabled || !settings.featureTypeProvider) {
            return null
        }

        val signatures = getSignatureSettings(e)
        if (signatures.isEmpty()) {
            return null
        }

        val matchedSignatures = getSignatureSetting(methodReference.name, signatures)
        if (matchedSignatures.isEmpty()) {
            return null
        }

        val refSignature = methodReference.signature
        if (StringUtil.isEmpty(refSignature)) {
            return null
        }

        // we need the param key on getBySignature(), since we are already in the resolved method there attach it to signature
        // param can have dotted values split with \
        val parameters = methodReference.parameters
        for (methodSignature in matchedSignatures) {
            if (parameters.size - 1 >= methodSignature.indexParameter) {
                val parameter = parameters[methodSignature.indexParameter]
                if (parameter is StringLiteralExpression) {
                    val param = parameter.contents
                    if (StringUtil.isNotEmpty(param)) {
                        return PhpType().add("#${key}$refSignature$METHOD_SIGNATURE_TRIM_KEY$param")
                    }
                }

                val parameterSignature = PhpTypeProviderUtil.getReferenceSignatureByFirstParameter(
                    methodReference,
                    METHOD_SIGNATURE_TRIM_KEY,
                )
                if (parameterSignature != null) {
                    return PhpType().add("#${key}$parameterSignature")
                }
            }
        }

        return null
    }

    override fun complete(s: String, project: Project): PhpType? = null

    private fun getSignatureSettings(psiElement: PsiElement): Collection<MethodSignatureSetting> {
        val signatures = mutableListOf<MethodSignatureSetting>()

        // get user defined settings
        val settings = Settings.getInstance(psiElement.project)
        if (settings.objectSignatureTypeProvider) {
            settings.methodSignatureSettings?.let(signatures::addAll)
        }

        // load extension
        val extensions = METHOD_SIGNATURE_EXTENSIONS.extensions
        if (extensions.isNotEmpty()) {
            val parameter = MethodSignatureTypeProviderParameter(psiElement)
            extensions.forEach { signatures.addAll(it.getSignatures(parameter)) }
        }

        return signatures
    }

    private fun getSignatureSetting(
        methodName: String?,
        methodSignatureSettings: Collection<MethodSignatureSetting>,
    ): Collection<MethodSignatureSetting> {
        return methodSignatureSettings.filter { it.methodName == methodName }
    }

    override fun getBySignature(
        expression: String,
        visited: Set<String>,
        depth: Int,
        project: Project,
    ): Collection<PhpNamedElement>? {
        // get back our original call
        val endIndex = expression.lastIndexOf(METHOD_SIGNATURE_TRIM_KEY)
        if (endIndex == -1) {
            return null
        }

        val originalSignature = expression.substring(0, endIndex)
        val unresolvedParameter = expression.substring(endIndex + 1)

        val phpIndex = PhpIndex.getInstance(project)
        val phpNamedElements = PhpTypeProviderUtil.getTypeSignature(phpIndex, originalSignature)
        if (phpNamedElements.isEmpty()) {
            return null
        }

        // get first matched item
        val phpNamedElement = phpNamedElements.first()
        if (phpNamedElement !is Method) {
            return null
        }

        val signatures = getSignatureSettings(phpNamedElement)
        if (signatures.isEmpty()) {
            return null
        }

        val parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, unresolvedParameter) ?: return null
        val resolvedElements = mutableListOf<PhpNamedElement>()

        for (matchedSignature in signatures) {
            for (signatureTypeProvider in PhpTypeSignatureTypes.DEFAULT_PROVIDER) {
                if (
                    signatureTypeProvider.name == matchedSignature.referenceProviderName &&
                    PhpElementsUtil.isMethodInstanceOf(
                        phpNamedElement,
                        matchedSignature.callTo,
                        matchedSignature.methodName,
                    )
                ) {
                    signatureTypeProvider.getByParameter(project, parameter)?.let(resolvedElements::addAll)
                }
            }
        }

        // not good but we need return any previous types: null clears all types
        return ArrayList(resolvedElements)
    }
}
