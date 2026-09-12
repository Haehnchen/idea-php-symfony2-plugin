package fr.adrienbrault.idea.symfony2plugin.templating.documentation

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.text.StringUtil
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.BinaryExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.elements.Variable
import com.jetbrains.twig.TwigLanguage
import com.jetbrains.twig.TwigTokenTypes
import com.jetbrains.twig.elements.TwigArrayLiteral
import com.jetbrains.twig.elements.TwigElementTypes
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.config.SymfonyPhpReferenceContributor
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateReference
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher

class TwigTemplateDocumentationTargetProvider : DocumentationTargetProvider {
    override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
        val context = resolveContext(file, offset) ?: return emptyList()

        return TwigUtil.getTemplateFiles(file.project, context.name).sortedBy { it.path }.map {
            TwigTemplateDocumentationTarget(context, it)
        }
    }

    internal fun resolveContext(file: PsiFile, offset: Int): TwigTemplateDocumentationContext? {
        if (!file.isValid || !Symfony2ProjectComponent.isEnabled(file.project) || DumbService.isDumb(file.project)) {
            return null
        }

        val context = if (file is PhpFile) {
            file.findElementAt(offset)?.let(::php)
        } else {
            val twig = file.viewProvider.getPsi(TwigLanguage.INSTANCE) ?: return null
            twig.findElementAt(offset)?.let(::twig)
        } ?: return null

        if (context.name.isBlank() || TwigUtil.getTemplateFiles(file.project, context.name).isEmpty()) {
            return null
        }

        return context
    }

    /**
     * Static arguments of TEMPLATE_SIGNATURES and existing Template references, for example:
     * ```php
     * $this->render('blog.html.twig');
     * $this->renderView(view: 'blog.html.twig', parameters: []);
     * $twig->load('blog.html.twig');
     * $email->htmlTemplate('blog.html.twig');
     * #[Template('blog.html.twig')]
     * #[Template(template: 'blog.html.twig')]
     * ```
     * Also supports the corresponding `@Template("blog.html.twig")` annotation.
     */
    private fun php(leaf: PsiElement): TwigTemplateDocumentationContext? {
        val literal = (leaf as? StringLiteralExpression)
            ?: (leaf.parent as? StringLiteralExpression)
            ?: return null

        // Exclude partial names: render('blog.html.twig' . $suffix), render("blog/$name.html.twig").
        if (literal.parent is BinaryExpression || PsiTreeUtil.findChildOfType(literal, Variable::class.java) != null) {
            return null
        }

        // Existing references also cover Template attributes and annotations.
        if (MethodMatcher.getMatchedSignatureWithDepth(literal, SymfonyPhpReferenceContributor.TEMPLATE_SIGNATURES) == null &&
            literal.references.none { it is TemplateReference }) {
            return null
        }

        return TwigTemplateDocumentationContext(literal.contents, literal)
    }

    /**
     * Static template strings, including their quote boundaries:
     * ```twig
     * {% extends 'layout.html.twig' %}
     * {% include 'blog.html.twig' %}
     * {% include ['blog.html.twig', 'fallback.html.twig'] %}
     * {% include condition ? 'blog.html.twig' : 'fallback.html.twig' %}
     * {% embed 'blog.html.twig' %}{% endembed %}
     * {% import 'forms.html.twig' as forms %}
     * {% from 'forms.html.twig' import field %}
     * {% form_theme form 'fields.html.twig' %}
     * {{ include('blog.html.twig') }}
     * {{ source('blog.html.twig') }}
     * {{ block('content', 'blog.html.twig') }}
     * ```
     * Excludes dynamic names: `'blog/' ~ name ~ '.html.twig'`, `"blog/#{name}.html.twig"`.
     */
    private fun twig(leaf: PsiElement): TwigTemplateDocumentationContext? {
        val anchor = when (leaf.node?.elementType) {
            TwigTokenTypes.STRING_TEXT -> leaf
            TwigTokenTypes.SINGLE_QUOTE, TwigTokenTypes.DOUBLE_QUOTE ->
                listOfNotNull(leaf.nextSibling, leaf.prevSibling).firstOrNull {
                    it.node?.elementType == TwigTokenTypes.STRING_TEXT
                }
            else -> null
        } ?: return null

        if (templateUsageType(anchor) == null) {
            return null
        }

        return TwigTemplateDocumentationContext(anchor.text, anchor)
    }
}

internal data class TwigTemplateDocumentationContext(val name: String, val anchor: PsiElement)

private val TEMPLATE_USAGE_PATTERNS = listOf(
    "extends" to TwigPattern.getTemplateFileReferenceTagPattern("extends"),
    "extends" to TwigPattern.getTagTernaryPattern(TwigElementTypes.EXTENDS_TAG),
    "include" to TwigPattern.getTemplateFileReferenceTagPattern("include"),
    "include" to TwigPattern.getIncludeTagArrayPattern(),
    "include" to TwigPattern.getTagTernaryPattern(TwigElementTypes.INCLUDE_TAG),
    "include" to TwigPattern.getPrintBlockOrTagFunctionPattern("include"),
    "embed" to TwigPattern.getTemplateFileReferenceTagPattern("embed"),
    "import" to TwigPattern.getTemplateFileReferenceTagPattern("import"),
    "from" to TwigPattern.getTemplateFileReferenceTagPattern("from"),
    "source" to TwigPattern.getPrintBlockOrTagFunctionPattern("source"),
    "form_theme" to TwigPattern.getFormThemeFileTagPattern(),
    "block" to TwigPattern.getPrintBlockOrTagFunctionSecondParameterPattern("block"),
)

internal fun templateUsageType(element: PsiElement): String? {
    if (element.node?.elementType != TwigTokenTypes.STRING_TEXT || !TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
        return null
    }

    // Current Twig PSI nests array strings below TwigArrayValue.
    val array = element.parent?.parent as? TwigArrayLiteral
    if (array != null) {
        val parentType = array.parent?.node?.elementType
        var previous = PsiTreeUtil.prevLeaf(array)
        while (previous != null && previous.text.isBlank()) {
            previous = PsiTreeUtil.prevLeaf(previous)
        }

        if (parentType == TwigElementTypes.INCLUDE_TAG && previous?.text == "include") {
            return "include"
        }

        if (array.parent.text.trimStart().startsWith("{% form_theme ") && previous?.text == "with") {
            return "form_theme"
        }
    }

    return TEMPLATE_USAGE_PATTERNS.firstOrNull { it.second.accepts(element) }?.first
}

internal data class TwigTemplateDocumentationData(
    val name: String,
    val paths: Map<String, String>, // Virtual-file URL -> presentable path; no retained PSI.
    val parents: Map<String, Set<String>> = emptyMap(),

    val phpFiles: Set<String> = emptySet(),
    val callers: Set<String> = emptySet(),

    val twigUsages: Map<String, Set<String>> = emptyMap(),
)

internal fun renderTwigTemplateDocumentation(data: TwigTemplateDocumentationData): String = buildString {
    fun escape(value: String) = StringUtil.escapeXmlEntities(value)
    fun code(value: String) = "<code>${escape(value)}</code>"

    append(DocumentationMarkup.CONTENT_START)

    for ((index, entry) in data.paths.entries.sortedBy { it.value }.withIndex()) {
        val (url, path) = entry
        if (index > 0) append("<br>")

        append("Path: ").append(code(path))
        data.parents[url]?.takeIf { it.isNotEmpty() }?.let {
            append("<br>Extends: ").append(it.sorted().joinToString(", ", transform = ::code))
        }
    }

    val roles = buildList {
        if (data.phpFiles.isNotEmpty()) add("PHP rendering")
        if (data.parents.values.any { it.isNotEmpty() }) add("extends")
        if (data.twigUsages.any { it.key != "extends" && it.value.isNotEmpty() }) add("included")
        if (!data.twigUsages["extends"].isNullOrEmpty()) add("extended")
    }

    if (roles.isNotEmpty()) {
        append("<br>Roles: ").append(roles.joinToString(", "))
    }

    if (data.paths.size > 1) {
        append("<br>Usages match template names; attribution to an individual path is ambiguous.")
    }

    val callers = data.callers.sorted()
    append("<br>PHP:")
    if (data.phpFiles.size != callers.size) {
        append(" ${data.phpFiles.size} files")
    }

    callers.take(5).forEach {
        append("<br>&nbsp;&nbsp;- ").append(code(it))
    }

    if (callers.size > 5) {
        append("<br>&nbsp;&nbsp;...<br>&nbsp;&nbsp;5 / ${callers.size}")
    }

    append("<br>Twig:")
    data.twigUsages.toSortedMap().forEach { (type, files) ->
        append("<br>&nbsp;&nbsp;- ").append(if (type == "extends") "Extended by" else escape(type))
            .append(": ${files.size} files")
    }

    if (data.phpFiles.isNotEmpty() || data.twigUsages.values.any { it.isNotEmpty() }) {
        append("<br><a href=\"").append(TEMPLATE_FIND_USAGES_LINK).append("\">Find usages</a>")
    }

    append(DocumentationMarkup.CONTENT_END)
}
