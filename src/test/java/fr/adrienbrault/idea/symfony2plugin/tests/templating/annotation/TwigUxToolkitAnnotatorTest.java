package fr.adrienbrault.idea.symfony2plugin.tests.templating.annotation;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.jetbrains.php.lang.highlighter.PhpHighlightingData;
import com.jetbrains.twig.TwigHighlighterData;
import fr.adrienbrault.idea.symfony2plugin.templating.annotation.TwigUxToolkitAnnotator;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.List;

/**
 * @see TwigUxToolkitAnnotator
 */
public class TwigUxToolkitAnnotatorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testInlinePropDocHighlightsMarkerAndType() {
        List<HighlightInfo> highlighting = highlight(
            "{%- props\n" +
            "    ## boolean Whether the item is open by default.\n" +
            "    open = false,\n" +
            "-%}"
        );

        // the ## marker is coloured like a PHPDoc @tag
        assertTrue(
            "Expected the ## marker highlighted",
            highlighting.stream().anyMatch(info ->
                info.getSeverity().getName().equals("INFORMATION") &&
                hasTextAttributesKey(info, PhpHighlightingData.DOC_TAG)
            )
        );

        // the type token is coloured like a PHPDoc type
        assertTrue(
            "Expected the prop type highlighted",
            highlighting.stream().anyMatch(info ->
                info.getSeverity().getName().equals("INFORMATION") &&
                hasTextAttributesKey(info, PhpHighlightingData.DOC_IDENTIFIER)
            )
        );
    }

    public void testInlinePropDocRepaintsTagBackground() {
        List<HighlightInfo> highlighting = highlight(
            "{%- props\n" +
            "    ## boolean Whether the item is open by default.\n" +
            "    open = false,\n" +
            "-%}"
        );

        // the {% %} tag background is repainted (the bundled Twig lexer drops it from the first '#')
        assertTrue(
            "Expected the tag background repainted with TWIG_TEMPLATE",
            highlighting.stream().anyMatch(info ->
                info.getSeverity().getName().equals("INFORMATION") &&
                hasTextAttributesKey(info, TwigHighlighterData.TWIG_TEMPLATE)
            )
        );
    }

    public void testInlinePropDocHighlightsDescriptionAsComment() {
        List<HighlightInfo> highlighting = highlight(
            "{%- props\n" +
            "    ## boolean Whether the item is open by default.\n" +
            "    open = false,\n" +
            "-%}"
        );

        assertTrue(
            "Expected the description rendered as a comment",
            highlighting.stream().anyMatch(info ->
                info.getSeverity().getName().equals("INFORMATION") &&
                hasTextAttributesKey(info, DefaultLanguageHighlighterColors.DOC_COMMENT)
            )
        );
    }

    public void testInlinePropDocTooltipShowsTypeDefaultAndDescription() {
        List<HighlightInfo> highlighting = highlight(
            "{%- props\n" +
            "    ## boolean Whether the item is open by default.\n" +
            "    open = false,\n" +
            "-%}"
        );

        assertTrue(
            "Expected a tooltip carrying the type, default value and description",
            highlighting.stream().anyMatch(info ->
                info.getToolTip() != null
                    && info.getToolTip().contains("boolean")
                    && info.getToolTip().contains("false")
                    && info.getToolTip().contains("Whether the item is open by default.")
            )
        );
    }

    public void testInlinePropDocTooltipOmitsDefaultForRequiredProp() {
        List<HighlightInfo> highlighting = highlight(
            "{%- props\n" +
            "    ## string Unique identifier.\n" +
            "    id,\n" +
            "-%}"
        );

        assertTrue(
            "Expected a tooltip carrying the type and description",
            highlighting.stream().anyMatch(info ->
                info.getToolTip() != null
                    && info.getToolTip().contains("string")
                    && info.getToolTip().contains("Unique identifier.")
            )
        );

        assertFalse(
            "A required prop must not show a default value",
            highlighting.stream().anyMatch(info ->
                info.getToolTip() != null && info.getToolTip().contains("= <code>")
            )
        );
    }

    public void testBlockDocMarkerIsHighlighted() {
        List<HighlightInfo> highlighting = highlight(
            "{## The item content. #}\n" +
            "{% block content %}{% endblock %}"
        );

        assertTrue(
            "Expected the {## marker highlighted as a documentation comment",
            highlighting.stream().anyMatch(info ->
                info.getSeverity().getName().equals("INFORMATION") &&
                hasTextAttributesKey(info, PhpHighlightingData.DOC_TAG)
            )
        );
    }

    public void testRegularCommentNotHighlighted() {
        List<HighlightInfo> highlighting = highlight("{# This is a regular comment #}");

        assertFalse(
            "A regular comment must not get documentation highlighting",
            highlighting.stream().anyMatch(info ->
                info.getSeverity().getName().equals("INFORMATION") &&
                hasTextAttributesKey(info, PhpHighlightingData.DOC_TAG)
            )
        );
    }

    private List<HighlightInfo> highlight(String content) {
        myFixture.configureByText("test.html.twig", content);
        return myFixture.doHighlighting();
    }

    /**
     * Helper method to check if a HighlightInfo has a specific TextAttributesKey.
     */
    private boolean hasTextAttributesKey(HighlightInfo info, TextAttributesKey expectedKey) {
        if (info.forcedTextAttributesKey != null) {
            return info.forcedTextAttributesKey.equals(expectedKey) ||
                   info.forcedTextAttributesKey.getFallbackAttributeKey() != null &&
                   info.forcedTextAttributesKey.getFallbackAttributeKey().equals(expectedKey);
        }
        return false;
    }
}
