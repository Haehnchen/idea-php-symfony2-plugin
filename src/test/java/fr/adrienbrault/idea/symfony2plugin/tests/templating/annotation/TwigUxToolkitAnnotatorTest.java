package fr.adrienbrault.idea.symfony2plugin.tests.templating.annotation;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import fr.adrienbrault.idea.symfony2plugin.templating.annotation.TwigUxToolkitAnnotator;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigUxToolkitAnnotator
 */
public class TwigUxToolkitAnnotatorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testPropAnnotationIsHighlighted() {
        myFixture.configureByText(
            "test.html.twig",
            "{# @prop open boolean Whether the item is open by default. #}"
        );

        List<HighlightInfo> highlighting = myFixture.doHighlighting();

        // Verify that highlighting is applied (INFORMATION level annotations from our annotator)
        assertTrue(
            "Expected highlighting for @prop annotation",
            highlighting.stream().anyMatch(info ->
                info.getSeverity().getName().equals("INFORMATION") &&
                hasTextAttributesKey(info, DefaultLanguageHighlighterColors.DOC_COMMENT_TAG)
            )
        );
    }

    public void testPropAnnotationHighlightsPropertyName() {
        myFixture.configureByText(
            "test.html.twig",
            "{# @prop open boolean Whether the item is open by default. #}"
        );

        List<HighlightInfo> highlighting = myFixture.doHighlighting();

        assertTrue(
            "Expected highlighting for property name",
            highlighting.stream().anyMatch(info ->
                info.getSeverity().getName().equals("INFORMATION") &&
                hasTextAttributesKey(info, DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE)
            )
        );
    }

    public void testPropAnnotationHighlightsType() {
        myFixture.configureByText(
            "test.html.twig",
            "{# @prop open boolean Whether the item is open by default. #}"
        );

        List<HighlightInfo> highlighting = myFixture.doHighlighting();

        assertTrue(
            "Expected highlighting for type",
            highlighting.stream().anyMatch(info ->
                info.getSeverity().getName().equals("INFORMATION") &&
                hasTextAttributesKey(info, DefaultLanguageHighlighterColors.CLASS_REFERENCE)
            )
        );
    }

    public void testBlockAnnotationIsHighlighted() {
        myFixture.configureByText(
            "test.html.twig",
            "{# @block content The item content. #}"
        );

        List<HighlightInfo> highlighting = myFixture.doHighlighting();

        assertTrue(
            "Expected highlighting for @block annotation",
            highlighting.stream().anyMatch(info ->
                info.getSeverity().getName().equals("INFORMATION") &&
                hasTextAttributesKey(info, DefaultLanguageHighlighterColors.DOC_COMMENT_TAG)
            )
        );
    }

    public void testBlockAnnotationHighlightsBlockName() {
        myFixture.configureByText(
            "test.html.twig",
            "{# @block content The item content. #}"
        );

        List<HighlightInfo> highlighting = myFixture.doHighlighting();

        assertTrue(
            "Expected highlighting for block name",
            highlighting.stream().anyMatch(info ->
                info.getSeverity().getName().equals("INFORMATION") &&
                hasTextAttributesKey(info, DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE)
            )
        );
    }

    public void testPropWithComplexType() {
        myFixture.configureByText(
            "test.html.twig",
            "{# @prop items App\\Entity\\Item[] List of items #}"
        );

        List<HighlightInfo> highlighting = myFixture.doHighlighting();

        assertTrue(
            "Expected highlighting for complex type",
            highlighting.stream().anyMatch(info ->
                info.getSeverity().getName().equals("INFORMATION") &&
                hasTextAttributesKey(info, DefaultLanguageHighlighterColors.CLASS_REFERENCE)
            )
        );
    }

    public void testPropWithUnionType() {
        myFixture.configureByText(
            "test.html.twig",
            "{# @prop value string|int|null The value #}"
        );

        List<HighlightInfo> highlighting = myFixture.doHighlighting();

        assertTrue(
            "Expected highlighting for union type",
            highlighting.stream().anyMatch(info ->
                info.getSeverity().getName().equals("INFORMATION") &&
                hasTextAttributesKey(info, DefaultLanguageHighlighterColors.CLASS_REFERENCE)
            )
        );
    }

    public void testRegularCommentNotHighlighted() {
        myFixture.configureByText(
            "test.html.twig",
            "{# This is a regular comment #}"
        );

        List<HighlightInfo> highlighting = myFixture.doHighlighting();

        // Regular comments should not have our specific highlighting
        assertFalse(
            "Regular comments should not have DOC_COMMENT_TAG highlighting",
            highlighting.stream().anyMatch(info ->
                info.getSeverity().getName().equals("INFORMATION") &&
                hasTextAttributesKey(info, DefaultLanguageHighlighterColors.DOC_COMMENT_TAG)
            )
        );
    }

    public void testVarCommentNotAffected() {
        // @var comments are handled by a different mechanism, ensure we don't interfere
        myFixture.configureByText(
            "test.html.twig",
            "{# @var foo \\App\\Entity\\Foo #}"
        );

        List<HighlightInfo> highlighting = myFixture.doHighlighting();

        // @var should not be highlighted by our annotator (it's not @prop or @block)
        assertFalse(
            "@var comments should not be highlighted by TwigUxToolkitAnnotator",
            highlighting.stream().anyMatch(info ->
                info.getSeverity().getName().equals("INFORMATION") &&
                hasTextAttributesKey(info, DefaultLanguageHighlighterColors.DOC_COMMENT_TAG) &&
                info.getText() != null && info.getText().contains("@var")
            )
        );
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
