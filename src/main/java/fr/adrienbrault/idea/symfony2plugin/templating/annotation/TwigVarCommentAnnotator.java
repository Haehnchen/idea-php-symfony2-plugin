package fr.adrienbrault.idea.symfony2plugin.templating.annotation;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.highlighter.PhpHighlightingData;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Syntax highlighting for Twig {@code @var} doc comment annotations.
 *
 * Supports both orderings and multiple declarations per comment block:
 * <ul>
 *   <li>{@code {# @var variable \AppBundle\Entity\Foo #}}</li>
 *   <li>{@code {# @var \AppBundle\Entity\Foo variable #}}</li>
 *   <li>Multi-line comments with multiple @var lines</li>
 * </ul>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil
 */
public class TwigVarCommentAnnotator implements Annotator {

    /**
     * {@code @var varName ClassName} — variable name comes first.
     * Group 1: {@code @var}, Group 2: variable name, Group 3: class name
     *
     * Example: {@code @var variable \AppBundle\Entity\Foo[]}
     */
    private static final Pattern VAR_FIRST_PATTERN = Pattern.compile(
        "(@var)\\s+(\\w+)\\s+([\\w\\\\\\[\\]]+)",
        Pattern.MULTILINE
    );

    /**
     * {@code @var ClassName varName} — class name (possibly FQCN) comes first.
     * Group 1: {@code @var}, Group 2: class name, Group 3: variable name
     *
     * Example: {@code @var \AppBundle\Entity\Foo[] variable}
     */
    private static final Pattern CLASS_FIRST_PATTERN = Pattern.compile(
        "(@var)\\s+([\\w\\\\\\[\\]]+)\\s+(\\w+)",
        Pattern.MULTILINE
    );

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!Symfony2ProjectComponent.isEnabled(element.getProject())) {
            return;
        }

        if (element.getNode().getElementType() != TwigTokenTypes.COMMENT_TEXT) {
            return;
        }

        String text = element.getText();
        if (!text.contains("@var")) {
            return;
        }

        int startOffset = element.getTextRange().getStartOffset();

        // Track which @var occurrences have been handled (by their start offset in the comment text).
        // VAR_FIRST_PATTERN takes priority — consistent with the existing completion/navigation logic.
        Set<Integer> handledAtVarStarts = new HashSet<>();

        Matcher varFirst = VAR_FIRST_PATTERN.matcher(text);
        while (varFirst.find()) {
            handledAtVarStarts.add(varFirst.start(1));
            // group 1 = @var, group 2 = var name, group 3 = class name
            highlight(holder, startOffset, varFirst, 1, PhpHighlightingData.DOC_TAG);
            highlight(holder, startOffset, varFirst, 2, PhpHighlightingData.VAR);
            highlight(holder, startOffset, varFirst, 3, PhpHighlightingData.CLASS);
        }

        Matcher classFirst = CLASS_FIRST_PATTERN.matcher(text);
        while (classFirst.find()) {
            if (handledAtVarStarts.contains(classFirst.start(1))) {
                continue;
            }
            // group 1 = @var, group 2 = class name, group 3 = var name
            highlight(holder, startOffset, classFirst, 1, PhpHighlightingData.DOC_TAG);
            highlight(holder, startOffset, classFirst, 2, PhpHighlightingData.CLASS);
            highlight(holder, startOffset, classFirst, 3, PhpHighlightingData.VAR);
        }
    }

    private void highlight(
        @NotNull AnnotationHolder holder,
        int baseOffset,
        @NotNull Matcher matcher,
        int group,
        @NotNull TextAttributesKey key
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
            .textAttributes(key)
            .create();
    }
}
