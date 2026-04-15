package fr.adrienbrault.idea.symfony2plugin.templating.usages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiDocumentManager
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateGoToDeclarationHandler

/**
 * Redirects Find Usages started from Twig type members to the already resolved PHP targets.
 *
 * Examples:
 * `{{ foo.id }}` on `id` -> `Bar::getId()` / `Bar::$id`
 * `{{ foo.id }}` on `foo` -> `Bar`
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigTypeFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
    /**
     * Activates the custom handler only when the Twig caret resolves to PHP class/member targets.
     */
    override fun canFindUsages(element: PsiElement): Boolean = getPhpTargets(element).isNotEmpty()

    /**
     * Stores the PHP delegation targets that should be searched instead of the raw Twig PSI.
     */
    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler? {
        val targets = getPhpTargets(element)

        return targets.takeIf { it.isNotEmpty() }?.let { TwigTypeFindUsagesHandler(element, it) }
    }

    companion object {
        /**
         * Reuses Twig goto resolution and keeps only PHP Find Usages targets.
         * Twig member lookups can arrive on a composite field reference, so nearby leaf candidates are probed as well.
         */
        fun getPhpTargets(element: PsiElement): List<PsiElement> {
            val resolvedCandidates = mutableListOf<Pair<PsiElement, List<PsiElement>>>()

            for (candidate in getCandidateElements(element)) {
                val resolvedTargets = TwigTemplateGoToDeclarationHandler.getTypeGoto(candidate)
                    .filter { it is PhpClass || it is Method || it is Field }

                if (resolvedTargets.isNotEmpty()) {
                    resolvedCandidates += candidate to resolvedTargets
                }
            }

            val preferredTargets = resolvedCandidates
                .maxWithOrNull(compareBy<Pair<PsiElement, List<PsiElement>>>(
                    { it.first.textOffset },
                    { if (it.second.any { target -> target !is PhpClass }) 1 else 0 },
                ))
                ?.second
                ?: emptyList()

            return preferredTargets
        }

        /**
         * Field references such as `detail.precisionFarming.summen.maisaussaatHa` are often passed as a single Twig PSI node.
         * Prefer the real caret leaf from the active editor so Find Usages only resolves the clicked Twig segment.
         */
        private fun getCandidateElements(element: PsiElement): Collection<PsiElement> {
            return getCaretElement(element)?.let(::listOf) ?: emptyList()
        }

        /**
         * When Find Usages is started from the editor, the platform still passes the whole Twig field chain.
         * Use the current caret offset to recover the concrete leaf the user clicked on.
         */
        private fun getCaretElement(element: PsiElement): PsiElement? {
            val project = element.project
            val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
            val containingFile = element.containingFile ?: return null
            val document = PsiDocumentManager.getInstance(project).getDocument(containingFile) ?: return null

            if (document != editor.document) {
                return null
            }

            val caretOffset = editor.caretModel.offset
            return containingFile.findElementAt(caretOffset)
        }
    }
}
