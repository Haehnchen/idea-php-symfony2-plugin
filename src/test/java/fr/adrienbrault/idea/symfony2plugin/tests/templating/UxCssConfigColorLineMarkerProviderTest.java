package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @see fr.adrienbrault.idea.symfony2plugin.templating.UxCssConfigColorLineMarkerProvider
 */
public class UxCssConfigColorLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("TwigFunctionArrayShapeCssReference.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    public void testRawColorTokenHasLineMarker() {
        PsiFile file = createConfig("""
            ux_css:
                tokens:
                    colors:
                        blue:
                            600: '#1572b6'
                        red: { 500: 'rgb(239 68 68)' }
                        green: 'oklch(72.3% 0.219 149.579)'
            """);

        assertLineMarker(file, new LineMarker.ToolTipEqualsAssert("#1572b6"));
        assertLineMarker(file, new LineMarker.ToolTipEqualsAssert("rgb(239 68 68)"));
        assertLineMarker(file, new LineMarker.ToolTipEqualsAssert("oklch(72.3% 0.219 149.579)"));
    }

    public void testColorTokenOfAnyConfigFileHasLineMarker() {
        PsiFile file = myFixture.addFileToProject("config/services.yaml", """
            ux_css:
                tokens:
                    colors:
                        blue: '#1572b6'
            """);

        assertLineMarker(file, new LineMarker.ToolTipEqualsAssert("#1572b6"));
    }

    public void testColorTokenOfPhpConfigHasLineMarker() {
        PsiFile file = myFixture.addFileToProject("config/packages/ux_css.php", """
            <?php
            return App::config([
                'ux_css' => [
                    'tokens' => [
                        'colors' => [
                            'blue' => [600 => '#1572b6'],
                        ],
                    ],
                    'semantic_tokens' => [
                        'colors' => [
                            'fg' => ['base' => '{colors.primary}'],
                        ],
                    ],
                ],
            ]);
            """);

        assertLineMarker(file, new LineMarker.ToolTipEqualsAssert("#1572b6"));
        assertLineMarker(file, new LineMarker.ToolTipEqualsAssert("primary: #3b82f6"));
    }

    public void testColorTokenOfPhpExtensionConfigHasLineMarker() {
        PsiFile file = myFixture.addFileToProject("config/packages/ux_css.php", """
            <?php
            return static function ($container) {
                $container->extension('ux_css', [
                    'tokens' => ['colors' => ['brand' => 'rgb(239 68 68)']],
                ]);
            };
            """);

        assertLineMarker(file, new LineMarker.ToolTipEqualsAssert("rgb(239 68 68)"));
    }

    public void testColorReferenceHasLineMarker() {
        PsiFile file = createConfig("""
            ux_css:
                semantic_tokens:
                    colors:
                        accent: '{colors.primary}'
                        fg:
                            base: '{colors.blue.500/50}'
            """);

        assertLineMarker(file, new LineMarker.ToolTipEqualsAssert("primary: #3b82f6"));
        assertLineMarker(file, new LineMarker.ToolTipEqualsAssert("blue.500/50: #3b82f6"));
    }

    public void testColorTokenOfEnvironmentHasLineMarker() {
        PsiFile file = createConfig("""
            when@dev:
                ux_css:
                    tokens:
                        colors:
                            debug: '#ff00ff'
            """);

        assertLineMarker(file, new LineMarker.ToolTipEqualsAssert("#ff00ff"));
    }

    public void testNoLineMarkerOutsideOfColorTokens() {
        assertLineMarkerIsEmpty(createConfig("""
            ux_css:
                tokens:
                    spacing:
                        md: '#1572b6'
                    colors:
                        unknown: '{colors.nope}'
                        current: 'currentColor'
            """));

        assertLineMarkerIsEmpty(myFixture.addFileToProject("config/packages/framework.yaml", """
            framework:
                ux_css:
                    tokens:
                        colors:
                            blue: '#1572b6'
            """));

        assertLineMarkerIsEmpty(myFixture.addFileToProject("src/Foo.php", """
            <?php
            $foo = ['ux_css' => ['tokens' => ['colors' => ['blue' => '#1572b6']]]];
            """));
    }

    private PsiFile createConfig(String content) {
        return myFixture.addFileToProject("config/packages/ux_css.yaml", content);
    }
}
