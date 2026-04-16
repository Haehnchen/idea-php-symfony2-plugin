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
import com.jetbrains.php.lang.psi.elements.PhpEnumCase
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.twig.TwigFile
import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil

/**
 * Adds Twig usages to Find Usages for PHP symbols that are referenced from Twig.
 *
 * Search paths are kept explicit inside one executor so the plugin only needs a single Twig
 * `referencesSearch` registration:
 * 1. Twig member paths like `foo.id`, `foo.getId()`, or `foo.primaryValue`
 * 2. Twig `constant('Foo\\Bar::BAZ')` calls for class constants and enum cases
 * 3. Twig class-name usages like `enum(...)`, `enum_cases(...)`, and `@var ... Class`
 *
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

        ApplicationManager.getApplication().runReadAction {
            when {
                target is Method && TwigTypeResolveUtil.isTwigAccessibleMethod(target) ->
                    doMethodSearch(target.project, target, queryParameters, consumer)

                target is Field && isTwigAccessibleField(target) ->
                    doFieldSearch(target.project, target, queryParameters, consumer)

                target is Field && target.isConstant ->
                    doConstantFieldSearch(target.project, target, queryParameters, consumer)

                target is PhpEnumCase ->
                    doEnumCaseSearch(target.project, target, queryParameters, consumer)

                target is PhpClass ->
                    doClassSearch(target.project, target, queryParameters, consumer)
            }
        }

        return true
    }

    /**
     * Twig exposes public non-constant fields as properties.
     */
    private fun isTwigAccessibleField(field: Field): Boolean {
        return field.modifier.isPublic && !field.isConstant
    }

    /**
     * Searches Twig method usages including property shortcuts like `foo.id` for `getId()`.
     */
    private fun doMethodSearch(
        project: Project,
        targetMethod: Method,
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ) {
        val processed = HashSet<PsiElement>()
        val searchWords = getTwigMethodSearchWords(targetMethod)
        if (searchWords.isEmpty()) {
            return
        }

        searchTwigFiles(project, queryParameters, searchWords) { twigPsiFile ->
            findTwigMethodUsages(twigPsiFile, targetMethod, processed, consumer)
            true
        }
    }

    /**
     * Searches Twig property usages backed by public PHP fields.
     */
    private fun doFieldSearch(
        project: Project,
        targetField: Field,
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ) {
        val processed = HashSet<PsiElement>()
        val searchWords = getTwigFieldSearchWords(targetField)
        if (searchWords.isEmpty()) {
            return
        }

        searchTwigFiles(project, queryParameters, searchWords) { twigPsiFile ->
            findTwigFieldUsages(twigPsiFile, targetField, processed, consumer)
            true
        }
    }

    /**
     * Searches Twig `constant(...)` calls for PHP class constants.
     */
    private fun doConstantFieldSearch(
        project: Project,
        target: Field,
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ) {
        val processed = HashSet<PsiElement>()
        val searchWords = TwigUsageTargetUtil.getTwigConstantFieldSearchWords(target)
        if (searchWords.isEmpty()) {
            return
        }

        searchTwigFiles(project, queryParameters, searchWords) { twigPsiFile ->
            twigPsiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (TwigPattern.getPrintBlockOrTagFunctionPattern("constant").accepts(element) &&
                        processed.add(element) &&
                        TwigUsageTargetUtil.resolvesToTwigConstantField(element, target)
                    ) {
                        consumer.process(TwigMethodUsageReference(element, target, TextRange(0, element.textLength)))
                    }

                    super.visitElement(element)
                }
            })

            true
        }
    }

    /**
     * Searches Twig `constant(...)` calls for PHP enum cases.
     */
    private fun doEnumCaseSearch(
        project: Project,
        target: PhpEnumCase,
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ) {
        val processed = HashSet<PsiElement>()
        val searchWords = TwigUsageTargetUtil.getTwigEnumCaseSearchWords(target)
        if (searchWords.isEmpty()) {
            return
        }

        searchTwigFiles(project, queryParameters, searchWords) { twigPsiFile ->
            twigPsiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (TwigPattern.getPrintBlockOrTagFunctionPattern("constant").accepts(element) &&
                        processed.add(element) &&
                        TwigUsageTargetUtil.resolvesToTwigEnumCase(element, target)
                    ) {
                        consumer.process(TwigMethodUsageReference(element, target, TextRange(0, element.textLength)))
                    }

                    super.visitElement(element)
                }
            })

            true
        }
    }

    /**
     * Searches Twig class-name usages that spell the PHP class directly.
     */
    private fun doClassSearch(
        project: Project,
        targetClass: PhpClass,
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ) {
        val shortName = targetClass.name
        searchTwigFiles(project, queryParameters, setOf(shortName)) { twigPsiFile ->
            twigPsiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
                override fun visitElement(element: PsiElement) {
                    // Match direct class-name usages in Twig enum helpers like `enum(...)` and `enum_cases(...)`.
                    if (TwigPattern.getPrintBlockOrTagFunctionPattern("enum", "enum_cases").accepts(element)) {
                        val fullRange = TextRange(0, element.textLength)
                        if (TwigUsageTargetUtil.getEnumTargets(element).any { TwigUsageTargetUtil.isClassTarget(it, targetClass) }) {
                            consumer.process(TwigMethodUsageReference(element, targetClass, fullRange))
                        }
                    }

                    for (match in TwigUsageTargetUtil.getVarClassMatches(element)) {
                        if (!TwigUsageTargetUtil.isClassTarget(match.targetClass, targetClass)) {
                            continue
                        }

                        consumer.process(TwigMethodUsageReference(element, targetClass, match.rangeInElement))
                    }

                    super.visitElement(element)
                }
            })

            true
        }
    }

    /**
     * Shared word-index prefilter for Twig files. Each search path keeps its own visitor logic,
     * while scope restriction and file resolution stay centralized here.
     */
    private fun searchTwigFiles(
        project: Project,
        queryParameters: ReferencesSearch.SearchParameters,
        searchWords: Set<String>,
        visitor: (TwigFile) -> Boolean,
    ) {
        val twigScope = getTwigSearchScope(project, queryParameters)
        val psiManager = PsiManager.getInstance(project)
        val searchHelper = PsiSearchHelper.getInstance(project)

        for (searchWord in searchWords) {
            searchHelper.processAllFilesWithWord(searchWord, twigScope, { psiFile: PsiFile ->
                if (psiFile !is TwigFile) {
                    return@processAllFilesWithWord true
                }

                val twigPsiFile = psiManager.findFile(psiFile.virtualFile)
                if (twigPsiFile !is TwigFile) {
                    return@processAllFilesWithWord true
                }

                visitor(twigPsiFile)
            }, false)
        }
    }

    private fun getTwigSearchScope(
        project: Project,
        queryParameters: ReferencesSearch.SearchParameters,
    ): GlobalSearchScope {
        val searchScope: SearchScope = queryParameters.effectiveSearchScope
        val scope = searchScope as? GlobalSearchScope ?: GlobalSearchScope.projectScope(project)
        return GlobalSearchScope.getScopeRestrictedByFileTypes(scope, TwigFileType.INSTANCE)
    }

    /**
     * Walks a candidate Twig file and emits synthetic references for matching method usages.
     */
    private fun findTwigMethodUsages(
        twigFile: TwigFile,
        targetMethod: Method,
        processed: MutableSet<PsiElement>,
        consumer: Processor<in PsiReference>,
    ) {
        val methodNames = getTwigMethodNames(targetMethod)

        twigFile.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                if (methodNames.isNotEmpty() &&
                    isTwigMethodCandidate(element, methodNames) &&
                    processed.add(element) &&
                    resolvesToTargetMethod(element, targetMethod)
                ) {
                    consumer.process(TwigMethodUsageReference(element, targetMethod, TextRange(0, element.textLength)))
                }

                super.visitElement(element)
            }
        })
    }

    /**
     * Walks a candidate Twig file and emits synthetic references for matching field usages.
     */
    private fun findTwigFieldUsages(
        twigFile: TwigFile,
        targetField: Field,
        processed: MutableSet<PsiElement>,
        consumer: Processor<in PsiReference>,
    ) {
        val fieldNames = getTwigFieldNames(targetField)

        twigFile.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                if (fieldNames.isNotEmpty() &&
                    isTwigMethodCandidate(element, fieldNames) &&
                    processed.add(element) &&
                    resolvesToTargetField(element, targetField)
                ) {
                    consumer.process(TwigMethodUsageReference(element, targetField, TextRange(0, element.textLength)))
                }

                super.visitElement(element)
            }
        })
    }

    /**
     * Filters down to Twig member leaves that textually match one of the supported method or field names.
     *
     * Examples:
     * `{{ bar.foo }}` -> leaf `foo`
     * `{{ bar.getFoo() }}` -> leaf `getFoo`
     */
    private fun isTwigMethodCandidate(element: PsiElement, twigNames: Set<String>): Boolean {
        if (!TwigPattern.getTypeCompletionPattern().accepts(element)) {
            return false
        }

        val text = element.text
        return twigNames.any { it.equals(text, ignoreCase = true) }
    }

    /**
     * Reuses Twig type resolution so only real, typed method accesses count as usages.
     */
    private fun resolvesToTargetMethod(element: PsiElement, targetMethod: Method): Boolean {
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
                if (isTargetMethod(target, targetMethod)) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Reuses Twig type resolution so only real, typed field accesses count as usages.
     */
    private fun resolvesToTargetField(element: PsiElement, targetField: Field): Boolean {
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
                if (isTargetField(target, targetField)) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Compares resolved Twig targets against the original PHP method declaration.
     */
    private fun isTargetMethod(candidate: PhpNamedElement, targetMethod: Method): Boolean {
        if (candidate.isEquivalentTo(targetMethod)) {
            return true
        }

        return candidate is Method && candidate.fqn == targetMethod.fqn
    }

    /**
     * Compares resolved Twig targets against the original PHP field declaration.
     */
    private fun isTargetField(candidate: PhpNamedElement, targetField: Field): Boolean {
        if (candidate.isEquivalentTo(targetField)) {
            return true
        }

        if (candidate !is Field || candidate.name != targetField.name) {
            return false
        }

        val candidateClass = candidate.containingClass ?: return false
        val targetClass = targetField.containingClass ?: return false

        return candidateClass.isEquivalentTo(targetClass) || candidateClass.fqn == targetClass.fqn
    }

    /**
     * Weak collection-like classes intentionally skip strict member validation in Twig-related inspections.
     */
    private fun isWeakPhpClass(phpClass: PhpClass): Boolean {
        return PhpElementsUtil.isInstanceOf(phpClass, "ArrayAccess") || PhpElementsUtil.isInstanceOf(phpClass, "Iterator")
    }

    /**
     * Collects all Twig-visible names for a PHP method, including getter shortcuts.
     */
    private fun getTwigMethodNames(method: Method): Set<String> {
        val names = linkedSetOf<String>()

        names.add(method.name)
        names.add(TwigTypeResolveUtil.getPropertyShortcutMethodName(method))

        return names.filter { it.isNotBlank() }.toCollection(linkedSetOf())
    }

    /**
     * Collects the Twig-visible property name for a public PHP field.
     */
    private fun getTwigFieldNames(field: Field): Set<String> {
        val names = linkedSetOf<String>()

        names.add(field.name)

        return names
    }

    /**
     * Prefetch words for Twig method search paths.
     */
    private fun getTwigMethodSearchWords(method: Method): Set<String> {
        val names = linkedSetOf<String>()

        names.addAll(getTwigMethodNames(method))

        return names.filter { it.isNotBlank() }.toCollection(linkedSetOf())
    }

    /**
     * Prefetch words for Twig field search paths.
     */
    private fun getTwigFieldSearchWords(field: Field): Set<String> {
        val names = linkedSetOf<String>()

        names.addAll(getTwigFieldNames(field))

        return names.filter { it.isNotBlank() }.toCollection(linkedSetOf())
    }

    /**
     * Synthetic reference that points a Twig usage back to the originating PHP member declaration.
     */
    class TwigMethodUsageReference(
        sourceElement: PsiElement,
        private val targetElement: PsiElement,
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
