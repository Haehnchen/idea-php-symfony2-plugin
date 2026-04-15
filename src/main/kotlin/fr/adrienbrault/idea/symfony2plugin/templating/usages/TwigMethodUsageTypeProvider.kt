package fr.adrienbrault.idea.symfony2plugin.templating.usages

import com.intellij.psi.PsiElement
import com.intellij.usages.PsiElementUsageTarget
import com.intellij.usages.UsageTarget
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProviderEx
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.twig.TwigFile

/**
 * Groups Twig member usages under a dedicated "Twig" bucket when Find Usages runs on a PHP method or property.
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
        if (!isMethodTarget(targets)) {
            return null
        }

        return if (element.containingFile is TwigFile) TWIG else null
    }
}

private val TWIG = UsageType { "Twig" }

/**
 * Restricts this provider to method/field Find Usages sessions.
 */
private fun isMethodTarget(targets: Array<out UsageTarget>): Boolean {
    for (target in targets) {
        if (target is PsiElementUsageTarget && (target.element is Method || target.element is Field)) {
            return true
        }
    }

    return false
}
