package fr.adrienbrault.idea.symfony2plugin.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.NewExpression
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.Variable
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4
import fr.adrienbrault.idea.symfony2plugin.Settings
import org.apache.commons.lang3.StringUtils

private const val EVENT_DISPATCHER_TRIM_KEY = '\u0197'

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class EventDispatcherTypeProvider : PhpTypeProvider4 {
    override fun getKey(): Char = '\u0187'

    // Index-safe only: no PhpIndex here.
    override fun getType(e: PsiElement): PhpType? {
        val methodReference = e as? MethodReference ?: return null

        val settings = Settings.getInstance(e.project)
        if (!settings.pluginEnabled || !settings.featureTypeProvider) {
            return null
        }

        // container calls are only on "get" methods
        if (methodReference.name != "dispatch") {
            return null
        }

        val parameters = methodReference.parameters
        if (parameters.size < 2) {
            return null
        }

        val refSignature = methodReference.signature
        if (StringUtils.isBlank(refSignature)) {
            return null
        }

        val signature = when (val parameter = parameters[1]) {
            // dispatch('foo', new FooEvent());
            is NewExpression -> parameter.classReference?.fqn?.takeUnless(StringUtils::isBlank) ?: return null

            // $event = new FooEvent();
            // dispatch('foo', $event);
            is Variable -> PhpElementsUtil.getFirstVariableTypeInScope(parameter) ?: return null

            else -> return null
        }

        return PhpType().add("#${key}$refSignature$EVENT_DISPATCHER_TRIM_KEY$signature")
    }

    override fun complete(s: String, project: Project): PhpType? = null

    override fun getBySignature(
        expression: String,
        visited: Set<String>,
        depth: Int,
        project: Project,
    ): Collection<PhpNamedElement> {
        // get back our original call
        // since phpstorm 7.1.2 we need to validate this
        val endIndex = expression.lastIndexOf(EVENT_DISPATCHER_TRIM_KEY)
        if (endIndex == -1) {
            return emptySet()
        }

        val originalSignature = expression.substring(0, endIndex)
        var parameter = expression.substring(endIndex + 1)

        if (!parameter.startsWith("\\")) {
            return emptySet()
        }

        val phpClass = PhpElementsUtil.getClass(project, parameter) ?: return emptySet()

        // search for called method
        val phpIndex = PhpIndex.getInstance(project)
        val phpNamedElements = PhpTypeProviderUtil.getTypeSignature(phpIndex, originalSignature)
        if (phpNamedElements.isEmpty()) {
            return emptySet()
        }

        // get first matched item
        val phpNamedElement = phpNamedElements.first()
        if (phpNamedElement !is Method) {
            return phpNamedElements
        }

        val containingClass = phpNamedElement.containingClass ?: return phpNamedElements

        parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, parameter) ?: return phpNamedElements

        // finally search the classes
        if (!PhpElementsUtil.isInstanceOf(containingClass, "\\Symfony\\Component\\EventDispatcher\\EventDispatcherInterface")) {
            return phpNamedElements
        }

        return listOf(phpClass)
    }
}
