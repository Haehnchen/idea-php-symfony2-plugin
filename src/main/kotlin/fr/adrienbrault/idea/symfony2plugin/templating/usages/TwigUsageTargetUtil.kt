package fr.adrienbrault.idea.symfony2plugin.templating.usages

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpEnumCase
import com.jetbrains.twig.TwigTokenTypes
import fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateGoToDeclarationHandler
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigConstantEnumResolver
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import java.util.regex.Pattern

/**
 * Shared Twig usage helpers for Find Usages handlers and ReferencesSearch executors.
 */
object TwigUsageTargetUtil {
    /**
     * Resolves all PHP targets that can start a Twig Find Usages session from the current caret leaf.
     */
    fun getTwigFindUsagesTargets(element: PsiElement): List<PsiElement> {
        val targets = linkedSetOf<PsiElement>()

        targets.addAll(TwigExtensionUsageUtil.getFunctionTargets(element))
        targets.addAll(TwigExtensionUsageUtil.getFilterTargets(element))
        targets.addAll(TwigTemplateGoToDeclarationHandler.getTypeGoto(element))
        targets.addAll(getConstantTargets(element))
        targets.addAll(getEnumTargets(element))
        targets.addAll(getVarClassTargets(element))

        return targets.toList()
    }

    /**
     * Resolves Twig `constant('Foo\\Bar::BAZ')` arguments to class constants or enum cases.
     */
    fun getConstantTargets(element: PsiElement): List<PsiElement> = TwigConstantEnumResolver.getConstantTargets(element).toList()

    /**
     * Resolves Twig `enum(...)` and `enum_cases(...)` arguments to the referenced enum class.
     */
    fun getEnumTargets(element: PsiElement): List<PhpClass> = TwigConstantEnumResolver.getEnumTargets(element).toList()

    /**
     * Resolves Twig `@var` declarations. `variable-first` keeps precedence over `class-first`,
     * mirroring the existing annotator and completion/navigation behavior.
     */
    fun getVarClassTargets(element: PsiElement): List<PhpClass> {
        return getVarClassMatches(element).map { it.targetClass }
    }

    /**
     * Extracts class usages and their precise ranges from Twig `@var` comments.
     */
    fun getVarClassMatches(element: PsiElement): List<TwigVarClassMatch> {
        if (element.node.elementType != TwigTokenTypes.COMMENT_TEXT) {
            return emptyList()
        }

        val text = element.text
        if (!text.contains("@var")) {
            return emptyList()
        }

        val matches = mutableListOf<TwigVarClassMatch>()
        val handledVarStarts = mutableSetOf<Int>()
        val patterns: Array<Pattern> = TwigTypeResolveUtil.INLINE_DOC_REGEX

        for ((index, pattern) in patterns.withIndex()) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val matchStart = matcher.start()
                if (index > 0 && handledVarStarts.contains(matchStart)) {
                    continue
                }

                handledVarStarts.add(matchStart)
                createVarClassMatch(
                    element = element,
                    rawClassName = matcher.group("class"),
                    startOffset = matcher.start("class"),
                    endOffset = matcher.end("class"),
                )?.let(matches::add)
            }
        }

        return matches
    }

    /**
     * Matches a resolved Twig `constant(...)` argument against the requested PHP constant field.
     */
    fun resolvesToTwigConstantField(element: PsiElement, target: Field): Boolean {
        return TwigConstantEnumResolver.getConstantTargetsForArgument(element)
            .filterIsInstance<Field>()
            .any { candidate -> candidate.isEquivalentTo(target) }
    }

    /**
     * Matches a resolved Twig `constant(...)` argument against the requested enum case.
     */
    fun resolvesToTwigEnumCase(element: PsiElement, target: PhpEnumCase): Boolean {
        return TwigConstantEnumResolver.getConstantTargetsForArgument(element)
            .filterIsInstance<PhpEnumCase>()
            .any { candidate -> candidate.isEquivalentTo(target) }
    }

    /**
     * Builds the cheap word-index prefilter terms for Twig constant field lookups.
     */
    fun getTwigConstantFieldSearchWords(target: Field): Set<String> {
        val names = linkedSetOf<String>()

        names.add(target.name)
        target.containingClass?.name?.let(names::add)

        return names.filter { it.isNotBlank() }.toCollection(linkedSetOf())
    }

    /**
     * Builds the cheap word-index prefilter terms for Twig enum-case lookups.
     */
    fun getTwigEnumCaseSearchWords(target: PhpEnumCase): Set<String> {
        val names = linkedSetOf<String>()

        names.add(target.name)
        target.containingClass?.name?.let(names::add)

        return names.filter { it.isNotBlank() }.toCollection(linkedSetOf())
    }

    /**
     * Compares a resolved Twig class target against the requested PHP class.
     */
    fun isClassTarget(candidate: PsiElement, targetClass: PhpClass): Boolean {
        return candidate is PhpClass && isSamePhpClass(candidate, targetClass)
    }

    /**
     * Compares two PHP classes by PSI identity first and only then by fully qualified name.
     */
    private fun isSamePhpClass(first: PhpClass, second: PhpClass): Boolean {
        if (first.isEquivalentTo(second)) {
            return true
        }

        return first.fqn == second.fqn
    }

    /**
     * Creates one resolved `@var` class match together with the precise class-name text range.
     */
    private fun createVarClassMatch(
        element: PsiElement,
        rawClassName: String,
        startOffset: Int,
        endOffset: Int,
    ): TwigVarClassMatch? {
        val className = rawClassName.removeSuffix("[]").replace("\\\\", "\\")
        val phpClass = PhpElementsUtil.getClassInterface(element.project, className) ?: return null

        return TwigVarClassMatch(
            targetClass = phpClass,
            rangeInElement = TextRange(startOffset, endOffset),
        )
    }
}

/**
 * One resolved Twig `@var` class usage together with the exact class-name range inside the comment token.
 *
 * Examples:
 * `{# @var cardSuite \Foo\CardSuite #}` -> range of `\Foo\CardSuite`
 * `{# @var \Foo\CardSuite cardSuite #}` -> range of `\Foo\CardSuite`
 */
data class TwigVarClassMatch(
    val targetClass: PhpClass,
    val rangeInElement: TextRange,
)
