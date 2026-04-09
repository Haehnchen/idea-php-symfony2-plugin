package fr.adrienbrault.idea.symfony2plugin.routing.usages

import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.usages.UsageTarget
import com.intellij.usages.UsageTargetProvider

/**
 * Provides the initial UsageTarget for route Find Usages.
 * This is the common entry point for both declaration-side searches and searches started from route usage strings.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class RouteUsageTargetProvider : UsageTargetProvider {
    /**
     * Resolves the caret to the owning route declaration target, regardless of whether the caret is on the declaration itself
     * or on a PHP/Twig route usage string.
     */
    override fun getTargets(element: PsiElement): Array<UsageTarget> {
        val usageTargets: Array<UsageTarget> = RouteUsageUtil.getRouteSearchTargets(element)
            .map { PsiElement2UsageTargetAdapter(it, true) }
            .toTypedArray()

        return if (usageTargets.isEmpty()) UsageTarget.EMPTY_ARRAY else usageTargets
    }

    /**
     * Editor-based entry point required by the platform. It forwards the caret PSI to the PSI-based overload.
     */
    override fun getTargets(editor: Editor, file: PsiFile): Array<UsageTarget>? {
        val elementAtCaret: PsiElement = file.findElementAt(editor.caretModel.offset) ?: return null
        val targets = getTargets(elementAtCaret)
        return targets.takeIf { it.isNotEmpty() }
    }
}
