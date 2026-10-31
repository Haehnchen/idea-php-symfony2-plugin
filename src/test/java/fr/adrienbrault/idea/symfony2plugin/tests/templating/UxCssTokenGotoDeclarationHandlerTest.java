package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.json.psi.JsonProperty;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * @see fr.adrienbrault.idea.symfony2plugin.templating.UxCssTokenGotoDeclarationHandler
 */
public class UxCssTokenGotoDeclarationHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("TwigFunctionArrayShape.php");
        myFixture.copyFileToProject("TwigFunctionArrayShapeCss.php");
        myFixture.copyFileToProject("TwigFunctionArrayShapeCssReference.php");
        myFixture.copyFileToProject("ux_css/ux_css.yaml", "config/packages/ux_css.yaml");
        myFixture.copyFileToProject("ux_css/panda-tokens.json", "vendor/symfony/ux-css/resources/panda-tokens.json");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    public void testColorTokenNavigatesToProjectConfigAndPandaDefault() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ color: 'blue.5<caret>00' }) }}", yamlKey("500", "blue"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ color: 'blue.5<caret>00' }) }}", jsonProperty("500", "blue"));
    }

    public void testColorTokenNavigatesToPandaDefault() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ color: 'red.5<caret>00' }) }}", jsonProperty("500", "red"));
    }

    public void testSemanticTokenNavigatesToProjectConfig() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ color: 'prim<caret>ary' }) }}", yamlKey("primary", "colors"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ color: 'f<caret>g' }) }}", yamlKey("fg", "colors"));
    }

    public void testTokenOfNestedConditionNavigates() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ _hover: { bg: 'bra<caret>nd' } }) }}", yamlKey("brand", "colors"));
    }

    public void testTokenOfConditionalValueNavigates() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ color: { base: 'bra<caret>nd' } }) }}", yamlKey("brand", "colors"));
    }

    public void testTokenWithModifiersNavigates() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ bg: 'bra<caret>nd/50' }) }}", yamlKey("brand", "colors"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ color: 'bra<caret>nd!' }) }}", yamlKey("brand", "colors"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ p: '-m<caret>d' }) }}", yamlKey("md", "spacing"));
    }

    public void testSpacingTokenNavigatesInItsCategory() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ p: 'm<caret>d' }) }}", yamlKey("md", "spacing"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ p: '1<caret>.5' }) }}", yamlKey("1.5", "spacing"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ p: '<caret>4' }) }}", jsonProperty("4", "spacing"));
    }

    public void testTokenNavigatesToConfigOfAnyYamlFile() {
        myFixture.copyFileToProject("ux_css/ux.yaml", "config/packages/ux.yaml");

        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ color: 'deb<caret>ug' }) }}", yamlKey("debug", "colors"));
    }

    public void testTokenNavigatesToPhpConfig() {
        myFixture.copyFileToProject("ux_css/ux_css.php", "config/packages/ux_css.php");
        myFixture.copyFileToProject("ux_css/ux_css_extension.php", "config/packages/ux_css_extension.php");

        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ color: 'acc<caret>ent' }) }}", phpKey("'accent'"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ color: 'gray.9<caret>00' }) }}", phpKey("900"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ css({ color: 'mu<caret>ted' }) }}", phpKey("'muted'"));
    }

    public void testNoNavigationOutsideOfTokens() {
        assertNavigationIsEmpty(TwigFileType.INSTANCE, "{{ css({ color: 'm<caret>d' }) }}");
        assertNavigationIsEmpty(TwigFileType.INSTANCE, "{{ css({ display: 'fl<caret>ex' }) }}");
        assertNavigationIsEmpty(TwigFileType.INSTANCE, "{{ css({ color: '[#ff<caret>6600]' }) }}");
        assertNavigationIsEmpty(TwigFileType.INSTANCE, "{{ button({ size: 'bra<caret>nd' }) }}");
    }

    private static PsiElementPattern.Capture<YAMLKeyValue> yamlKey(String key, String parentKey) {
        return PlatformPatterns.psiElement(YAMLKeyValue.class)
            .withName(key)
            .withSuperParent(2, PlatformPatterns.psiElement(YAMLKeyValue.class).withName(parentKey));
    }

    private static PsiElementPattern.Capture<PsiElement> phpKey(String text) {
        return PlatformPatterns.psiElement().withText(text).withSuperParent(2, ArrayHashElement.class);
    }

    private static PsiElementPattern.Capture<JsonProperty> jsonProperty(String key, String parentKey) {
        return PlatformPatterns.psiElement(JsonProperty.class)
            .withName(key)
            .withSuperParent(2, PlatformPatterns.psiElement(JsonProperty.class).withName(parentKey));
    }
}
