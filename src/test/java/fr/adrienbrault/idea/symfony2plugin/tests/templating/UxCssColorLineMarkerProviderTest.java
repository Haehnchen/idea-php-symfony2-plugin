package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @see fr.adrienbrault.idea.symfony2plugin.templating.UxCssColorLineMarkerProvider
 */
public class UxCssColorLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("TwigFunctionArrayShape.php");
        myFixture.copyFileToProject("TwigFunctionArrayShapeCss.php");
        myFixture.copyFileToProject("TwigFunctionArrayShapeCssReference.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    public void testColorTokenOfCssFunctionHasLineMarker() {
        assertLineMarker(
            myFixture.configureByText(TwigFileType.INSTANCE, "<div class=\"{{ css({ color: 'blue.500' }) }}\"></div>"),
            new LineMarker.ToolTipEqualsAssert("blue.500: #3b82f6")
        );
    }

    public void testColorTokenOfNestedConditionHasLineMarker() {
        assertLineMarker(
            myFixture.configureByText(TwigFileType.INSTANCE, "{{ css({ _hover: { bg: 'primary' } }) }}"),
            new LineMarker.ToolTipEqualsAssert("primary: #3b82f6")
        );
    }

    public void testColorTokenOfConditionalValueHasLineMarker() {
        assertLineMarker(
            myFixture.configureByText(TwigFileType.INSTANCE, "{{ css({ color: { base: 'primary' } }) }}"),
            new LineMarker.ToolTipEqualsAssert("primary: #3b82f6")
        );
    }

    public void testColorTokenWithOpacityHasLineMarker() {
        assertLineMarker(
            myFixture.configureByText(TwigFileType.INSTANCE, "{{ css({ bg: 'blue.500/50' }) }}"),
            new LineMarker.ToolTipEqualsAssert("blue.500/50: #3b82f6")
        );
    }

    public void testRawColorValueHasLineMarker() {
        assertLineMarker(
            myFixture.configureByText(TwigFileType.INSTANCE, "{{ css({ color: '[#ff6600]' }) }}"),
            new LineMarker.ToolTipEqualsAssert("[#ff6600]: #ff6600")
        );
    }

    public void testNoLineMarkerForValuesThatAreNotColors() {
        assertLineMarkerIsEmpty(myFixture.configureByText(TwigFileType.INSTANCE, "{{ css({ color: 'unknown', display: 'flex' }) }}"));
        assertLineMarkerIsEmpty(myFixture.configureByText(TwigFileType.INSTANCE, "{{ css({ color: 'current' }) }}"));
    }

    public void testNoLineMarkerOutsideOfCssFunction() {
        assertLineMarkerIsEmpty(myFixture.configureByText(TwigFileType.INSTANCE, "{{ button({ size: 'blue.500' }) }}"));
        assertLineMarkerIsEmpty(myFixture.configureByText(TwigFileType.INSTANCE, "{{ 'blue.500' }}"));
    }
}
