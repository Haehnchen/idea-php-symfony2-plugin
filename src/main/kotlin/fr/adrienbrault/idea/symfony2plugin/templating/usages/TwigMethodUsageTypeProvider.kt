package fr.adrienbrault.idea.symfony2plugin.templating.usages

import com.intellij.psi.PsiElement
import com.intellij.usages.PsiElementUsageTarget
import com.intellij.usages.UsageTarget
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProviderEx
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpEnumCase
import com.jetbrains.twig.TwigFile

/**
 * Groups synthetic Twig usages under a dedicated "Twig" bucket for PHP symbol searches.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigMethodUsageTypeProvider : UsageTypeProviderEx {
    /**
     * Legacy API entry point. The target-aware overload below is the one used in practice.
     */
    override fun getUsageType(element: PsiElement): UsageType? = null

    /**
     * Marks Twig hits only when the current Usage View belongs to a PHP member search.
     */
    override fun getUsageType(element: PsiElement, targets: Array<out UsageTarget>): UsageType? {
        if (!isTwigUsageTarget(targets)) {
            return null
        }

        return if (element.containingFile is TwigFile) TWIG else null
    }
}

private val TWIG = UsageType { "Twig" }

/**
 * Restricts this provider to PHP Find Usages sessions that can produce Twig synthetic references.
 */
private fun isTwigUsageTarget(targets: Array<out UsageTarget>): Boolean {
    for (target in targets) {
        if (target !is PsiElementUsageTarget) {
            continue
        }

        // Symbol-based Twig extension usage targets like `form_start` in `{{ form_start() }}` should stay in the Twig bucket.
        if (target.element.containingFile is TwigFile && TwigExtensionUsageUtil.getTwigExtensionSymbol(target.element) != null) {
            return true
        }

        // Limit Twig usage grouping to the concrete PHP symbol kinds supported by the Twig search executor.
        if (target.element is Method || target.element is Field || target.element is PhpEnumCase || target.element is PhpClass) {
            return true
        }
    }

    return false
}
