package fr.adrienbrault.idea.symfony2plugin.templating.usages

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.twig.TwigFile
import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil

/**
 * Adds Twig variable-path usages to Find Usages for PHP methods and public properties.
 * Candidate files are prefetched via the word index and then resolved with the same Twig type pipeline.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigMethodReferencesSearchExecutor : QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {
    /**
     * Starts the custom Twig-member usage search only for Twig-accessible PHP members.
     */
    override fun execute(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ): Boolean {
        val target = queryParameters.elementToSearch
        val targetMember = getTwigTargetMember(target) ?: return true

        ApplicationManager.getApplication().runReadAction {
            doSearch(targetMember.project, targetMember, queryParameters, consumer)
        }

        return true
    }

    /**
     * Narrows the search target to PHP members that can actually be referenced from Twig.
     */
    private fun getTwigTargetMember(target: PsiElement): PhpNamedElement? {
        if (target is Method && TwigTypeResolveUtil.isTwigAccessibleMethod(target)) {
            return target
        }

        if (target is Field && isTwigAccessibleField(target)) {
            return target
        }

        return null
    }

    /**
     * Twig exposes public non-constant fields as properties.
     */
    private fun isTwigAccessibleField(field: Field): Boolean {
        return field.modifier.isPublic && !field.isConstant
    }

    /**
     * Uses the word index as a cheap prefilter before resolving concrete Twig member accesses.
     */
    private fun doSearch(
        project: Project,
        targetMember: PhpNamedElement,
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ) {
        val searchScope: SearchScope = queryParameters.effectiveSearchScope
        val scope = searchScope as? GlobalSearchScope ?: GlobalSearchScope.projectScope(project)
        val twigScope = GlobalSearchScope.getScopeRestrictedByFileTypes(scope, TwigFileType.INSTANCE)
        val psiManager = PsiManager.getInstance(project)
        val processed = HashSet<PsiElement>()
        val twigNames = getTwigMemberNames(targetMember)
        if (twigNames.isEmpty()) {
            return
        }

        val searchHelper = PsiSearchHelper.getInstance(project)
        for (twigName in twigNames) {
            searchHelper.processAllFilesWithWord(twigName, twigScope, { psiFile: PsiFile ->
                // The word prefilter may still return non-Twig files from the wider scope.
                if (psiFile !is TwigFile) {
                    return@processAllFilesWithWord true
                }

                val twigPsiFile = psiManager.findFile(psiFile.virtualFile)
                if (twigPsiFile !is TwigFile) {
                    return@processAllFilesWithWord true
                }

                findTwigMethodUsages(twigPsiFile, twigNames, targetMember, processed, consumer)
                true
            }, false)
        }
    }

    /**
     * Walks a candidate Twig file and emits synthetic references for matching member accesses.
     */
    private fun findTwigMethodUsages(
        twigFile: TwigFile,
        twigNames: Set<String>,
        targetMember: PhpNamedElement,
        processed: MutableSet<PsiElement>,
        consumer: Processor<in PsiReference>,
    ) {
        twigFile.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                if (isTwigMethodCandidate(element, twigNames) &&
                    processed.add(element) &&
                    resolvesToTargetMember(element, targetMember)
                ) {
                    consumer.process(TwigMethodUsageReference(element, targetMember, TextRange(0, element.textLength)))
                }

                super.visitElement(element)
            }
        })
    }

    /**
     * Filters down to Twig member leaves that textually match one of the supported names.
     */
    private fun isTwigMethodCandidate(element: PsiElement, twigNames: Set<String>): Boolean {
        if (!TwigPattern.getTypeCompletionPattern().accepts(element)) {
            return false
        }

        val text = element.text
        return twigNames.any { it.equals(text, ignoreCase = true) }
    }

    /**
     * Reuses Twig type resolution so only real, typed member accesses count as usages.
     */
    private fun resolvesToTargetMember(element: PsiElement, targetMember: PhpNamedElement): Boolean {
        val beforeLeaf = TwigTypeResolveUtil.formatPsiTypeName(element)
        if (beforeLeaf.isEmpty()) {
            return false
        }

        val types = TwigTypeResolveUtil.resolveTwigMethodName(element, beforeLeaf)
        if (types.isEmpty()) {
            return false
        }

        for (twigTypeContainer in types) {
            val phpNamedElement = twigTypeContainer.phpNamedElement ?: continue
            if (phpNamedElement is PhpClass && isWeakPhpClass(phpNamedElement)) {
                continue
            }

            for (target in TwigTypeResolveUtil.getTwigPhpNameTargets(phpNamedElement, element.text)) {
                if (isTargetMember(target, targetMember)) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Compares resolved Twig targets against the original PHP declaration.
     */
    private fun isTargetMember(candidate: PhpNamedElement, targetMember: PhpNamedElement): Boolean {
        if (candidate.isEquivalentTo(targetMember)) {
            return true
        }

        return when (candidate) {
            is Method if targetMember is Method -> candidate.fqn == targetMember.fqn
            is Field if targetMember is Field ->
                candidate.name == targetMember.name && candidate.containingClass?.fqn == targetMember.containingClass?.fqn

            else -> false
        }
    }

    /**
     * Weak collection-like classes intentionally skip strict member validation in Twig-related inspections.
     */
    private fun isWeakPhpClass(phpClass: PhpClass): Boolean {
        return PhpElementsUtil.isInstanceOf(phpClass, "ArrayAccess") || PhpElementsUtil.isInstanceOf(phpClass, "Iterator")
    }

    /**
     * Collects all Twig-visible names for a PHP member, including getter shortcuts.
     */
    private fun getTwigMemberNames(member: PhpNamedElement): Set<String> {
        val names = linkedSetOf<String>()

        when (member) {
            is Method -> {
                names.add(member.name)
                names.add(TwigTypeResolveUtil.getPropertyShortcutMethodName(member))
            }
            is Field -> names.add(member.name)
        }

        return names.filter { it.isNotBlank() }.toCollection(linkedSetOf())
    }

    /**
     * Synthetic reference that points a Twig usage back to the originating PHP member declaration.
     */
    class TwigMethodUsageReference(
        sourceElement: PsiElement,
        private val targetElement: PhpNamedElement,
        rangeInElement: TextRange,
    ) : PsiReferenceBase<PsiElement>(sourceElement, rangeInElement, false) {
        /**
         * Resolves the synthetic usage directly to the PHP member under Find Usages.
         */
        override fun resolve(): PsiElement = targetElement

        /**
         * These synthetic references are not used for completion.
         */
        override fun getVariants(): Array<Any> = emptyArray()
    }
}
