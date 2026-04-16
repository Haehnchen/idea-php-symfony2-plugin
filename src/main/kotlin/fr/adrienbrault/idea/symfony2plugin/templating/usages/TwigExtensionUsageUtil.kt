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
     * Resolves the current Twig caret leaf to one logical Twig extension symbol.
     */
    fun getTwigExtensionSymbol(element: PsiElement): TwigExtensionUsageSymbol? {
        return getFunctionSymbol(element) ?: getFilterSymbol(element)
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
     * Resolves a Twig function usage leaf to its canonical function symbol name.
     */
    fun getFunctionSymbol(element: PsiElement): TwigExtensionUsageSymbol? {
        if (!TwigPattern.getPrintBlockFunctionPattern().accepts(element)) {
            return null
        }

        val name = getRegisteredExtensionName(element.text, TwigExtensionParser.getFunctions(element.project)) ?: return null
        return TwigExtensionUsageSymbol(TwigExtensionUsageKind.FUNCTION, name)
    }

    /**
     * Resolves a Twig filter usage leaf to its canonical filter symbol name.
     */
    fun getFilterSymbol(element: PsiElement): TwigExtensionUsageSymbol? {
        if (!TwigPattern.getFilterPattern().accepts(element) && !TwigPattern.getApplyFilterPattern().accepts(element)) {
            return null
        }

        val name = getRegisteredExtensionName(element.text, TwigExtensionParser.getFilters(element.project)) ?: return null
        return TwigExtensionUsageSymbol(TwigExtensionUsageKind.FILTER, name)
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
     * Finds the canonical registered Twig extension name for one symbol leaf.
     */
    private fun getRegisteredExtensionName(
        twigName: String,
        extensions: Map<String, TwigExtension>,
    ): String? {
        if (twigName.isBlank()) {
            return null
        }

        return extensions.keys.firstOrNull { it.equals(twigName, ignoreCase = true) }
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

/**
 * Twig extension symbol kind like `form_start` in `{{ form_start() }}` or `trans` in `{{ value|trans }}`.
 */
enum class TwigExtensionUsageKind {
    /** Twig function like `form_start` in `{{ form_start() }}`. */
    FUNCTION,

    /** Twig filter like `trans` in `{{ value|trans }}`. */
    FILTER,
}

/**
 * One logical Twig extension symbol like `form_start` in `{{ form_start() }}`.
 */
data class TwigExtensionUsageSymbol(
    /** Symbol kind such as function or filter. */
    val kind: TwigExtensionUsageKind,

    /** Canonical registered Twig name like `form_start` or `trans`. */
    val name: String,
)
