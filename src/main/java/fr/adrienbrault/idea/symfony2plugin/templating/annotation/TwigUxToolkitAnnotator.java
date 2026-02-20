package fr.adrienbrault.idea.symfony2plugin.templating.annotation;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.jetbrains.php.lang.highlighter.PhpHighlightingData;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides syntax highlighting for Symfony UX Toolkit Twig comment annotations.
 *
 * Supports:
 * - {# @prop name type Description #}
 * - {# @block name Description #}
 *
 * Uses PHP's PHPDoc highlighting colors for consistency with `@property` annotations.
 *
 * @see <a href="https://github.com/symfony/ux-toolkit">Symfony UX Toolkit</a>
 */
public class TwigUxToolkitAnnotator implements Annotator {
    /**
     * Pattern for @prop annotations.
     * Format: @prop name type Description
     * Example: @prop open boolean Whether the item is open by default.
     *
     * Supports complex types:
     * - Simple: string, boolean, int
     * - Nullable: ?string, string|null
     * - Union: string|int|null
     * - Generic: array<string>, Collection<int, Item>
     * - Literal strings: 'vertical'|'horizontal'
     * - FQCN: App\Entity\Item, \DateTime
     * - Arrays: string[], Item[]
     *
     * @see <a href="https://regex101.com/r/3JXNX7/1">Regex101</a>
     */
    private static final Pattern PROP_PATTERN = Pattern.compile(
        "(@prop)\\s+(\\w+)\\s+(\\S+)\\s+(.+?)\\s*$",
        Pattern.DOTALL
    );

    /**
     * Pattern for @block annotations.
     * Format: @block name Description
     * Example: @block content The item content.
     *
     * @see <a href="https://regex101.com/r/jYjXpq/1">Regex101</a>
     */
    private static final Pattern BLOCK_PATTERN = Pattern.compile(
        "(@block)\\s+(\\w+)\\s+(.+?)\\s*$",
        Pattern.DOTALL
    );

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!Symfony2ProjectComponent.isEnabled(element.getProject())) {
            return;
        }

        // Only process Twig comment text tokens
        if (element.getNode().getElementType() != TwigTokenTypes.COMMENT_TEXT) {
            return;
        }

        String text = element.getText();
        int startOffset = element.getTextRange().getStartOffset();

        // Try to match @prop pattern
        Matcher propMatcher = PROP_PATTERN.matcher(text);
        if (propMatcher.find()) {
            annotateProp(holder, startOffset, propMatcher, getPropDefaults(element));
            return;
        }

        // Try to match @block pattern
        Matcher blockMatcher = BLOCK_PATTERN.matcher(text);
        if (blockMatcher.find()) {
            annotateBlock(holder, startOffset, blockMatcher);
        }
    }

    /**
     * Annotates a @prop comment with syntax highlighting.
     * Highlights: @prop keyword, property name, and type.
     *
     * Uses PHP PHPDoc colors:
     * - DOC_TAG for @prop keyword (like @property in PHPDoc)
     * - DOC_PROPERTY_IDENTIFIER for property name (like $foo in @property string $foo)
     * - DOC_IDENTIFIER for type (like string in @property string $foo)
     *
     * The property name also carries a hover tooltip with its type, default value (sourced from the
     * {@code {% props %}} tag, the single source of truth for defaults) and description.
     */
    private void annotateProp(@NotNull AnnotationHolder holder, int startOffset, @NotNull Matcher matcher, @NotNull Map<String, String> propDefaults) {
        // Highlight @prop keyword (group 1) - like @property
        highlightRange(holder, startOffset, matcher, 1, PhpHighlightingData.DOC_TAG);

        // Highlight property name (group 2) - like $foo in @property string $foo - with a hover tooltip
        String name = matcher.group(2);
        String defaultValue = name != null ? propDefaults.get(name) : null;
        String tooltip = buildPropTooltip(name, matcher.group(3), matcher.group(4), defaultValue);
        highlightRangeWithTooltip(holder, startOffset, matcher, 2, PhpHighlightingData.DOC_PROPERTY_IDENTIFIER, tooltip);

        // Highlight type (group 3) - like string in @property string $foo
        highlightRange(holder, startOffset, matcher, 3, PhpHighlightingData.DOC_IDENTIFIER);
    }

    /**
     * Reads the prop default values declared in the template's {@code {% props %}} tag, cached per file.
     */
    private static @NotNull Map<String, String> getPropDefaults(@NotNull PsiElement element) {
        PsiFile file = element.getContainingFile();
        if (!(file instanceof TwigFile twigFile)) {
            return Collections.emptyMap();
        }

        return CachedValuesManager.getCachedValue(twigFile, () ->
            CachedValueProvider.Result.create(UxUtil.getComponentTemplatePropDefaults(twigFile), twigFile)
        );
    }

    /**
     * Builds the hover tooltip for a prop: {@code name : type = default} followed by its description.
     */
    private static @NotNull String buildPropTooltip(@Nullable String name, @Nullable String type, @Nullable String description, @Nullable String defaultValue) {
        StringBuilder tooltip = new StringBuilder();

        if (name != null) {
            tooltip.append("<code>").append(StringUtil.escapeXmlEntities(name)).append("</code>");
        }

        if (type != null) {
            tooltip.append(" : <code>").append(StringUtil.escapeXmlEntities(type)).append("</code>");
        }

        if (defaultValue != null) {
            tooltip.append(" = <code>").append(StringUtil.escapeXmlEntities(defaultValue)).append("</code>");
        }

        if (description != null && !description.isBlank()) {
            tooltip.append("<br/>").append(StringUtil.escapeXmlEntities(description.trim().replaceAll("\\s+", " ")));
        }

        return tooltip.toString();
    }

    /**
     * Like {@link #highlightRange} but attaches a hover tooltip (which {@code newSilentAnnotation} cannot carry).
     */
    private void highlightRangeWithTooltip(
        @NotNull AnnotationHolder holder,
        int baseOffset,
        @NotNull Matcher matcher,
        int group,
        @NotNull TextAttributesKey textAttributesKey,
        @NotNull String tooltip
    ) {
        if (matcher.group(group) == null) {
            return;
        }

        TextRange range = new TextRange(
            baseOffset + matcher.start(group),
            baseOffset + matcher.end(group)
        );

        holder.newAnnotation(HighlightSeverity.INFORMATION, StringUtil.removeHtmlTags(tooltip))
            .range(range)
            .tooltip(tooltip)
            .textAttributes(textAttributesKey)
            .create();
    }

    /**
     * Annotates a @block comment with syntax highlighting.
     * Highlights: @block keyword and block name.
     *
     * Uses PHP PHPDoc colors:
     * - DOC_TAG for @block keyword
     * - DOC_PROPERTY_IDENTIFIER for block name
     */
    private void annotateBlock(@NotNull AnnotationHolder holder, int startOffset, @NotNull Matcher matcher) {
        // Highlight @block keyword (group 1)
        highlightRange(holder, startOffset, matcher, 1, PhpHighlightingData.DOC_TAG);

        // Highlight block name (group 2)
        highlightRange(holder, startOffset, matcher, 2, PhpHighlightingData.DOC_PROPERTY_IDENTIFIER);
    }

    /**
     * Creates a silent annotation with the specified text attributes for a regex group match.
     */
    private void highlightRange(
        @NotNull AnnotationHolder holder,
        int baseOffset,
        @NotNull Matcher matcher,
        int group,
        @NotNull TextAttributesKey textAttributesKey
    ) {
        if (matcher.group(group) == null) {
            return;
        }

        TextRange range = new TextRange(
            baseOffset + matcher.start(group),
            baseOffset + matcher.end(group)
        );

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range)
            .textAttributes(textAttributesKey)
            .create();
    }
}
