package fr.adrienbrault.idea.symfony2plugin.templating.annotation;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

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
            annotateProp(holder, startOffset, propMatcher);
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
     */
    private void annotateProp(@NotNull AnnotationHolder holder, int startOffset, @NotNull Matcher matcher) {
        // Highlight @prop keyword (group 1) - like @property
        highlightRange(holder, startOffset, matcher, 1, DefaultLanguageHighlighterColors.DOC_COMMENT_TAG);

        // Highlight property name (group 2) - like $foo in @property string $foo
        highlightRange(holder, startOffset, matcher, 2, DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE);

        // Highlight type (group 3) - like string in @property string $foo
        highlightRange(holder, startOffset, matcher, 3, DefaultLanguageHighlighterColors.CLASS_REFERENCE);
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
        highlightRange(holder, startOffset, matcher, 1, DefaultLanguageHighlighterColors.DOC_COMMENT_TAG);

        // Highlight block name (group 2)
        highlightRange(holder, startOffset, matcher, 2, DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE);
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
