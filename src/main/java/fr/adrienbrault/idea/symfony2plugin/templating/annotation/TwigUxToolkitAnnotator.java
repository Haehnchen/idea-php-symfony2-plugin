package fr.adrienbrault.idea.symfony2plugin.templating.annotation;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.jetbrains.php.lang.highlighter.PhpHighlightingData;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigHighlighterData;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Syntax highlighting for Symfony UX Toolkit Twig documentation comments (Twig 3.29 {@code ##} comments):
 *
 * <ul>
 *   <li>inline prop docs {@code ## <type> <description>} inside a {@code {% props %}} tag: the type token is
 *       coloured like a PHPDoc type and carries a tooltip with the prop's default value and description;</li>
 *   <li>block docs {@code {## <description> #}} preceding a {@code {% block %}}: the {@code ##} marker is
 *       coloured to set it apart from a regular {@code {# #}} comment.</li>
 * </ul>
 *
 * <p>The bundled Twig lexer does not tokenize inline {@code ##} comments inside a {@code {% %}} block, so
 * prop docs are highlighted by scanning the tag's raw text rather than from comment PSI tokens.
 *
 * @see <a href="https://github.com/symfony/ux-toolkit">Symfony UX Toolkit</a>
 * @see <a href="https://github.com/twigphp/Twig/pull/4871">twigphp/Twig#4871</a>
 */
public class TwigUxToolkitAnnotator implements Annotator {
    /** Head of a {@code {% props %}} tag, used to skip every other Twig tag quickly. */
    private static final Pattern PROPS_TAG_HEAD = Pattern.compile("^\\{%-?\\s*props\\b");

    /** A whole inline prop-doc line: group 1 the {@code ##} marker, group 2 the type token. */
    private static final Pattern PROP_DOC_LINE = Pattern.compile("(##)[ \\t]*(\\S+)?[^\\n]*");

    /** An identifier, used to find the prop that an inline doc line documents. */
    private static final Pattern IDENTIFIER = Pattern.compile("\\w+");

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!Symfony2ProjectComponent.isEnabled(element.getProject())) {
            return;
        }

        IElementType elementType = element.getNode().getElementType();
        if (elementType == TwigElementTypes.TAG) {
            annotatePropsTag(element, holder);
        } else if (elementType == TwigTokenTypes.COMMENT_TEXT) {
            annotateBlockDoc(element, holder);
        }
    }

    /**
     * Colours each inline {@code ## <type> <description>} prop doc inside a {@code {% props %}} tag: the
     * {@code ##} marker (like a PHPDoc {@code @tag}) and the type token (like a PHPDoc type), the latter
     * with a tooltip resolved from the documented prop.
     */
    private void annotatePropsTag(@NotNull PsiElement tag, @NotNull AnnotationHolder holder) {
        String text = tag.getText();
        if (!PROPS_TAG_HEAD.matcher(text).find()) {
            return;
        }

        int base = tag.getTextRange().getStartOffset();

        Matcher matcher = PROP_DOC_LINE.matcher(text);
        if (!matcher.find()) {
            return;
        }

        // The bundled Twig lexer stops applying the {% %} tag background from the first inline '#'
        // onward; repaint it (background only) across the whole tag so the doc comments do not punch a
        // hole in the tag background.
        highlight(holder, base, base + text.length(), TwigHighlighterData.TWIG_TEMPLATE, null);

        Map<String, UxUtil.TwigComponentProp> props = getProps(tag);

        do {
            highlight(holder, base + matcher.start(1), base + matcher.end(1), PhpHighlightingData.DOC_TAG, null);

            int descriptionStart = matcher.end(1);
            if (matcher.group(2) != null) {
                UxUtil.TwigComponentProp prop = props.get(nextPropName(text, matcher.end(), props.keySet()));
                String tooltip = prop != null ? buildPropTooltip(prop) : null;
                highlight(holder, base + matcher.start(2), base + matcher.end(2), PhpHighlightingData.DOC_IDENTIFIER, tooltip);
                descriptionStart = matcher.end(2);
            }

            // render the description like a comment so the type stands out (the bundled lexer would
            // otherwise mis-tokenize these words as code inside the {% props %} tag)
            if (matcher.end() > descriptionStart) {
                highlight(holder, base + descriptionStart, base + matcher.end(), DefaultLanguageHighlighterColors.DOC_COMMENT, null);
            }
        } while (matcher.find());
    }

    /**
     * Colours the {@code ##} marker of a block doc comment {@code {## <description> #}} (which the Twig
     * lexer exposes as a {@code COMMENT_TEXT} starting with {@code #}) to set it apart from a plain comment.
     */
    private void annotateBlockDoc(@NotNull PsiElement commentText, @NotNull AnnotationHolder holder) {
        String text = commentText.getText();
        if (!text.startsWith("#")) {
            return;
        }

        int start = commentText.getTextRange().getStartOffset();
        int markerEnd = text.length() > 1 && text.charAt(1) == '#' ? 2 : 1;
        highlight(holder, start, start + markerEnd, PhpHighlightingData.DOC_TAG, null);
    }

    private static @Nullable String nextPropName(@NotNull String text, int from, @NotNull Set<String> names) {
        Matcher matcher = IDENTIFIER.matcher(text);
        if (!matcher.find(from)) {
            return null;
        }

        do {
            if (names.contains(matcher.group())) {
                return matcher.group();
            }
        } while (matcher.find());

        return null;
    }

    private static @NotNull Map<String, UxUtil.TwigComponentProp> getProps(@NotNull PsiElement element) {
        PsiFile file = element.getContainingFile();
        if (!(file instanceof TwigFile twigFile)) {
            return Collections.emptyMap();
        }

        return CachedValuesManager.getCachedValue(twigFile, () ->
            CachedValueProvider.Result.create(UxUtil.getComponentTemplateProps(twigFile), twigFile)
        );
    }

    private static @NotNull String buildPropTooltip(@NotNull UxUtil.TwigComponentProp prop) {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<code>").append(StringUtil.escapeXmlEntities(prop.name())).append("</code>");

        if (!prop.type().isBlank()) {
            tooltip.append(" : <code>").append(StringUtil.escapeXmlEntities(prop.type())).append("</code>");
        }
        if (prop.defaultValue() != null) {
            tooltip.append(" = <code>").append(StringUtil.escapeXmlEntities(prop.defaultValue())).append("</code>");
        }
        if (!prop.description().isBlank()) {
            tooltip.append("<br/>").append(StringUtil.escapeXmlEntities(prop.description()));
        }

        return tooltip.toString();
    }

    /**
     * Silent when there is no tooltip; otherwise an INFORMATION annotation that can carry one
     * ({@code newSilentAnnotation} cannot).
     */
    private void highlight(@NotNull AnnotationHolder holder, int start, int end, @NotNull TextAttributesKey key, @Nullable String tooltip) {
        TextRange range = new TextRange(start, end);

        if (tooltip == null) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(range)
                .textAttributes(key)
                .create();
        } else {
            holder.newAnnotation(HighlightSeverity.INFORMATION, StringUtil.removeHtmlTags(tooltip))
                .range(range)
                .tooltip(tooltip)
                .textAttributes(key)
                .create();
        }
    }
}
