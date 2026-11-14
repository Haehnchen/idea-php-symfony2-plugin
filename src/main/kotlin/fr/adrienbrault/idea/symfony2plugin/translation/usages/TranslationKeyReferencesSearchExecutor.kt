package fr.adrienbrault.idea.symfony2plugin.translation.usages

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.SyntaxTraverser
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.twig.TwigFile
import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil

/**
 * A usage is any PHP or Twig string the plugin already navigates to this translation key in this domain.
 */
class TranslationKeyReferencesSearchExecutor : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val scope = queryParameters.effectiveSearchScope as? GlobalSearchScope ?: return
        val declaration = TranslationKeyUsageUtil.getDeclaration(queryParameters.elementToSearch) ?: return
        val project = declaration.element.project

        val candidateFiles = mutableListOf<VirtualFile>()
        PsiSearchHelper.getInstance(project).processCandidateFilesForText(
            GlobalSearchScope.getScopeRestrictedByFileTypes(scope, PhpFileType.INSTANCE, TwigFileType.INSTANCE),
            UsageSearchContext.ANY,
            true,
            declaration.key,
        ) { candidateFiles.add(it) }

        val psiManager = PsiManager.getInstance(project)
        for (virtualFile in candidateFiles) {
            val psiFile = psiManager.findFile(virtualFile) ?: continue
            for ((element, range) in collectUsages(psiFile, declaration)) {
                if (!consumer.process(TranslationKeyUsageReference(element, declaration.element, range))) {
                    return
                }
            }
        }
    }

    private fun collectUsages(psiFile: PsiFile, declaration: TranslationKeyDeclaration): List<Pair<PsiElement, TextRange>> {
        val usages = mutableListOf<Pair<PsiElement, TextRange>>()

        if (psiFile is PhpFile) {
            for (literal in PsiTreeUtil.findChildrenOfType(psiFile, StringLiteralExpression::class.java)) {
                val leaf = literal.firstChild
                if (literal.contents == declaration.key && leaf != null && (isPhpTranslationCall(literal, declaration) || isNavigationTarget(leaf, declaration))) {
                    usages.add(literal to literal.valueRange)
                }
            }
        } else if (psiFile is TwigFile) {
            // {% trans %}key{% endtrans %} lives in the template data language, not in the Twig tree
            for (root in psiFile.viewProvider.allFiles) {
                for (leaf in SyntaxTraverser.psiTraverser(root).filter { it.firstChild == null && it.textMatches(declaration.key) }) {
                    if (isTwigTranslationFilter(leaf, declaration) || isNavigationTarget(leaf, declaration)) {
                        usages.add(leaf to TextRange(0, leaf.textLength))
                    }
                }
            }
        }

        return usages
    }

    private fun isPhpTranslationCall(literal: StringLiteralExpression, declaration: TranslationKeyDeclaration): Boolean =
        TranslationUtil.getTranslationDomainForKey(literal) == declaration.domain

    private fun isTwigTranslationFilter(leaf: PsiElement, declaration: TranslationKeyDeclaration): Boolean =
        TwigPattern.getTranslationKeyPattern("trans", "transchoice", "trans_html").accepts(leaf)
            && TwigUtil.getPsiElementTranslationDomain(leaf) == declaration.domain

    // forms, constraints, {% trans %}: their GotoCompletion targets reach this file only when the domain matches
    private fun isNavigationTarget(leaf: PsiElement, declaration: TranslationKeyDeclaration): Boolean {
        val declarationFile = declaration.element.containingFile.virtualFile ?: return false

        return GotoCompletionUtil.getContributors(leaf).asSequence()
            .mapNotNull { it.getProvider(leaf) }
            .flatMap { it.getPsiTargets(leaf) }
            .any { it.navigationElement.containingFile?.virtualFile == declarationFile }
    }

    class TranslationKeyUsageReference(
        sourceElement: PsiElement,
        private val targetElement: PsiElement,
        rangeInElement: TextRange,
    ) : PsiReferenceBase<PsiElement>(sourceElement, rangeInElement, false) {
        override fun resolve(): PsiElement = targetElement

        override fun getVariants(): Array<Any> = emptyArray()
    }
}
