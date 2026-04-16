package fr.adrienbrault.idea.symfony2plugin.templating.usages

import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.Method
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser

/**
 * Shared Twig extension usage helpers for Find Usages delegation and reverse Twig search from PHP methods.
 */
object TwigExtensionUsageUtil {
    /**
     * Resolves a Twig function usage leaf like `importmap` in `{{ importmap() }}` to PHP method targets.
     */
    fun getFunctionTargets(element: PsiElement): List<Method> {
        if (!TwigPattern.getPrintBlockFunctionPattern().accepts(element)) {
            return emptyList()
        }

        return getMethodTargetsForName(element, TwigExtensionParser.getFunctions(element.project))
    }

    /**
     * Resolves a Twig filter usage leaf like `foo` in `{{ value|foo }}` or `{% apply foo %}` to PHP method targets.
     */
    fun getFilterTargets(element: PsiElement): List<Method> {
        if (!TwigPattern.getFilterPattern().accepts(element) && !TwigPattern.getApplyFilterPattern().accepts(element)) {
            return emptyList()
        }

        return getMethodTargetsForName(element, TwigExtensionParser.getFilters(element.project))
    }

    /**
     * Returns all Twig function names that currently resolve back to the given PHP method.
     */
    fun getFunctionNames(targetMethod: Method): Set<String> {
        return getExtensionNames(targetMethod, TwigExtensionParser.getFunctions(targetMethod.project))
    }

    /**
     * Returns all Twig filter names that currently resolve back to the given PHP method.
     */
    fun getFilterNames(targetMethod: Method): Set<String> {
        return getExtensionNames(targetMethod, TwigExtensionParser.getFilters(targetMethod.project))
    }

    /**
     * Cheap guard for method-based references search to decide whether Twig extension search branches should run.
     */
    fun hasExtensionNames(targetMethod: Method): Boolean {
        return getFunctionNames(targetMethod).isNotEmpty() || getFilterNames(targetMethod).isNotEmpty()
    }

    /**
     * Resolves one concrete Twig extension name to all matching PHP methods. Multiple matches are preserved.
     */
    private fun getMethodTargetsForName(
        element: PsiElement,
        extensions: Map<String, TwigExtension>,
    ): List<Method> {
        val methods = linkedSetOf<Method>()
        val twigName = element.text
        if (twigName.isBlank()) {
            return emptyList()
        }

        for ((extensionName, twigExtension) in extensions) {
            if (!extensionName.equals(twigName, ignoreCase = true)) {
                continue
            }

            val target = TwigExtensionParser.getExtensionTarget(element.project, twigExtension) as? Method ?: continue
            methods.add(target)
        }

        return methods.toList()
    }

    /**
     * Reverses the Twig extension registry and collects every extension name that points at the requested method.
     */
    private fun getExtensionNames(
        targetMethod: Method,
        extensions: Map<String, TwigExtension>,
    ): Set<String> {
        val names = linkedSetOf<String>()

        for ((extensionName, twigExtension) in extensions) {
            val target = TwigExtensionParser.getExtensionTarget(targetMethod.project, twigExtension) as? Method ?: continue
            if (isTargetMethod(target, targetMethod)) {
                names.add(extensionName)
            }
        }

        return names
    }

    /**
     * Compares PHP methods by PSI identity first and then by method name plus containing class.
     */
    private fun isTargetMethod(candidate: Method, targetMethod: Method): Boolean {
        if (candidate.isEquivalentTo(targetMethod)) {
            return true
        }

        if (candidate.name != targetMethod.name) {
            return false
        }

        val candidateClass = candidate.containingClass ?: return false
        val targetClass = targetMethod.containingClass ?: return false

        return candidateClass.isEquivalentTo(targetClass) || candidateClass.fqn == targetClass.fqn
    }
}
