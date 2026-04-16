package fr.adrienbrault.idea.symfony2plugin.templating.usages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.usages.UsageTarget
import com.intellij.usages.UsageTargetProvider
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpEnumCase
import com.jetbrains.twig.TwigFileType

/**
 * Redirects Find Usages started from Twig symbols to the already resolved PHP targets.
 *
 * Examples:
 * `{{ foo.id }}` on `id` -> `Bar::getId()` / `Bar::$id`
 * `{{ foo.id }}` on `foo` -> `Bar`
 * `{{ constant('Foo\\Bar::BAZ') }}` on `BAZ` -> `Foo\Bar::BAZ`
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigFindUsagesHandlerFactory : FindUsagesHandlerFactory(), UsageTargetProvider {
    /**
     * Allows Find Usages to start directly on Twig usage PSI by exposing either Twig symbol targets
     * or delegated PHP targets depending on the resolved usage identity.
     */
    override fun getTargets(element: PsiElement): Array<UsageTarget> {
        val target = getTwigFindUsagesTarget(element) ?: return UsageTarget.EMPTY_ARRAY
        val usageTargets: Array<UsageTarget> = target.primaryElements
            .map { PsiElement2UsageTargetAdapter(it, true) }
            .toTypedArray()

        return if (usageTargets.isEmpty()) UsageTarget.EMPTY_ARRAY else usageTargets
    }

    /**
     * Editor-based entry point required by the platform. It forwards the caret PSI to the PSI-based overload.
     */
    override fun getTargets(editor: Editor, file: PsiFile): Array<UsageTarget>? {
        val elementAtCaret = file.findElementAt(editor.caretModel.offset) ?: return null
        return getTargets(elementAtCaret).takeIf { it.isNotEmpty() }
    }

    /**
     * Activates the custom handler only when the Twig caret resolves to a supported Twig/PHP usage identity.
     */
    override fun canFindUsages(element: PsiElement): Boolean = getTwigFindUsagesTarget(element) != null

    /**
     * Stores the effective Twig or PHP search targets for the Find Usages session.
     */
    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler? {
        return getTwigFindUsagesTarget(element)?.let { TwigFindUsagesHandler(it) }
    }
}

/**
 * Resolves the Find Usages identity for one Twig caret position.
 * Extension symbols stay Twig-symbol based, while all other cases keep delegating to PHP.
 */
private fun getTwigFindUsagesTarget(element: PsiElement): TwigFindUsagesTarget? {
    if (element.containingFile?.fileType != TwigFileType.INSTANCE) {
        return null
    }

    val resolvedCandidates = mutableListOf<Pair<PsiElement, List<PsiElement>>>()
    val candidateElements = getTwigFindUsagesCaretElement(element)?.let(::listOf) ?: listOf(element)

    for (candidate in candidateElements) {
        TwigExtensionUsageUtil.getTwigExtensionSymbol(candidate)?.let { symbol ->
            return TwigExtensionSymbolFindUsagesTarget(candidate, symbol)
        }

        val resolvedTargets = TwigUsageTargetUtil.getTwigFindUsagesTargets(candidate)
            .filter { it is PhpClass || it is Method || it is Field || it is PhpEnumCase }

        if (resolvedTargets.isNotEmpty()) {
            resolvedCandidates += candidate to resolvedTargets
        }
    }

    return resolvedCandidates
        .maxWithOrNull(compareBy<Pair<PsiElement, List<PsiElement>>>(
            { it.first.textOffset },
            { if (it.second.any { target -> target !is PhpClass }) 1 else 0 },
        ))
        ?.let { TwigPhpFindUsagesTarget(it.first, it.second) }
}

/**
 * When Find Usages is started from the editor, the platform still passes the whole Twig field chain.
 * Use the current caret offset to recover the concrete leaf the user clicked on.
 */
private fun getTwigFindUsagesCaretElement(element: PsiElement): PsiElement? {
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
