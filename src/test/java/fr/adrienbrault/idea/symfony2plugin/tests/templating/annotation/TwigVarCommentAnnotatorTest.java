package fr.adrienbrault.idea.symfony2plugin.tests.templating.annotation;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.jetbrains.php.lang.highlighter.PhpHighlightingData;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.annotation.TwigVarCommentAnnotator;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigVarCommentAnnotator
 */
public class TwigVarCommentAnnotatorTest extends SymfonyLightCodeInsightFixtureTestCase {

    // -- @var keyword ----------------------------------------------------------

    public void testVarFirstHighlightsAtVarKeyword() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{# @var variable \\AppBundle\\Entity\\Foo #}");
        assertHighlightKey(PhpHighlightingData.DOC_TAG);
    }

    public void testClassFirstHighlightsAtVarKeyword() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{# @var \\AppBundle\\Entity\\Foo variable #}");
        assertHighlightKey(PhpHighlightingData.DOC_TAG);
    }

    // -- variable name (purple, same as $var in PHP code) ---------------------

    public void testVarFirstHighlightsVariableName() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{# @var variable \\AppBundle\\Entity\\Foo #}");
        assertHighlightKey(PhpHighlightingData.VAR);
    }

    public void testClassFirstHighlightsVariableName() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{# @var \\AppBundle\\Entity\\Foo variable #}");
        assertHighlightKey(PhpHighlightingData.VAR);
    }

    // -- class name ------------------------------------------------------------

    public void testVarFirstHighlightsClassName() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{# @var variable \\AppBundle\\Entity\\Foo #}");
        assertHighlightKey(PhpHighlightingData.CLASS);
    }

    public void testClassFirstHighlightsClassName() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{# @var \\AppBundle\\Entity\\Foo variable #}");
        assertHighlightKey(PhpHighlightingData.CLASS);
    }

    public void testArrayClassTypeHighlighted() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{# @var items \\App\\Entity\\Item[] #}");
        assertHighlightKey(PhpHighlightingData.CLASS);
    }

    // -- multiple @var declarations in one comment block -----------------------

    public void testMultipleVarDeclarationsAllHighlighted() {
        myFixture.configureByText(TwigFileType.INSTANCE,
            "{#\n @var foo \\App\\Foo\n @var bar \\App\\Bar\n#}"
        );

        List<HighlightInfo> highlighting = myFixture.doHighlighting();

        long tagCount = highlighting.stream()
            .filter(info -> hasKey(info, PhpHighlightingData.DOC_TAG))
            .count();
        assertTrue("Expected at least 2 @var keyword highlights for two declarations", tagCount >= 2);

        long classCount = highlighting.stream()
            .filter(info -> hasKey(info, PhpHighlightingData.CLASS))
            .count();
        assertTrue("Expected at least 2 class highlights for two declarations", classCount >= 2);
    }

    public void testMultipleVarDeclarationsMixedOrderHighlighted() {
        myFixture.configureByText(TwigFileType.INSTANCE,
            "{#\n @var \\App\\Foo foo\n @var bar \\App\\Bar\n#}"
        );

        List<HighlightInfo> highlighting = myFixture.doHighlighting();

        long tagCount = highlighting.stream()
            .filter(info -> hasKey(info, PhpHighlightingData.DOC_TAG))
            .count();
        assertTrue("Expected at least 2 @var keyword highlights for mixed-order declarations", tagCount >= 2);
    }

    // -- negative: plain comments should not be highlighted --------------------

    public void testPlainCommentNotHighlighted() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{# This is a regular comment #}");

        List<HighlightInfo> highlighting = myFixture.doHighlighting();

        assertFalse(
            "Regular comments should not have PHP_DOC_TAG highlighting",
            highlighting.stream().anyMatch(info -> hasKey(info, PhpHighlightingData.DOC_TAG))
        );
    }

    // -- helpers ---------------------------------------------------------------

    private void assertHighlightKey(@NotNull TextAttributesKey key) {
        List<HighlightInfo> highlighting = myFixture.doHighlighting();
        assertTrue(
            "Expected highlighting with key: " + key.getExternalName(),
            highlighting.stream().anyMatch(info -> hasKey(info, key))
        );
    }

    private static boolean hasKey(@NotNull HighlightInfo info, @NotNull TextAttributesKey expectedKey) {
        if (info.forcedTextAttributesKey == null) {
            return false;
        }
        if (info.forcedTextAttributesKey.equals(expectedKey)) {
            return true;
        }
        TextAttributesKey fallback = info.forcedTextAttributesKey.getFallbackAttributeKey();
        return fallback != null && fallback.equals(expectedKey);
    }
}
