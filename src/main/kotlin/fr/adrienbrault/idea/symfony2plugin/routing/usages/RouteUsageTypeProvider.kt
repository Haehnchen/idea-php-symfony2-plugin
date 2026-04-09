package fr.adrienbrault.idea.symfony2plugin.routing.usages

import com.intellij.psi.PsiElement
import com.intellij.usages.PsiElementUsageTarget
import com.intellij.usages.UsageTarget
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProviderEx
import com.jetbrains.php.lang.psi.PhpFile
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigRouteUsageStubIndex

/**
 * Groups route usages in the Usage View into the user-facing buckets shown in the tree.
 * It only activates when the current Find Usages target is a route declaration.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class RouteUsageTypeProvider : UsageTypeProviderEx {
    /**
     * Legacy API entry point. IntelliJ prefers the target-aware overload below.
     */
    override fun getUsageType(element: PsiElement): UsageType? = null

    /**
     * Classifies each route usage only when the current search target is a route declaration.
     */
    override fun getUsageType(element: PsiElement, targets: Array<out UsageTarget>): UsageType? {
        if (!isRouteDeclarationTarget(targets)) {
            return null
        }

        if (element.containingFile is PhpFile) {
            return PHP
        }

        return if (TwigRouteUsageStubIndex.getUsageKind(element) != null) TWIG else null
    }

    companion object {
        private val TWIG = UsageType { "Twig" }
        private val PHP = UsageType { "PHP" }

        /**
         * Confirms that the current Usage View really belongs to a route declaration target.
         */
        private fun isRouteDeclarationTarget(targets: Array<out UsageTarget>): Boolean {
            for (target in targets) {
                val element = getTargetElement(target)
                if (element != null && RouteUsageUtil.getRouteNameForDeclaration(element) != null) {
                    return true
                }
            }

            return false
        }

        /**
         * Unwraps synthetic usage targets back to the declaration PSI used by the search.
         */
        private fun getTargetElement(target: UsageTarget): PsiElement? {
            if (target !is PsiElementUsageTarget) {
                return null
            }

            val element = target.element
            return if (element is RouteFindUsagesHandler.RouteFindUsagesNavigationItem) {
                element.delegate
            } else {
                element
            }
        }
    }
}
