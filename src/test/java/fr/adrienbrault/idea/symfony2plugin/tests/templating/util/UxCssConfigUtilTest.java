package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.jetbrains.php.lang.psi.PhpFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.UxCssConfigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

public class UxCssConfigUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testConditionalPhpConfig() {
        PhpFile file = (PhpFile) myFixture.addFileToProject("config/packages/ux.php", """
            <?php
            return App::config(['when@dev' => ['ux_css' => ['semantic_tokens' => []]]]);
            """);

        assertSize(1, UxCssConfigUtil.getPhpConfigs(file));
        assertEquals("['semantic_tokens' => []]", UxCssConfigUtil.getPhpConfigs(file).getFirst().getText());
    }

    public void testPhpConfigsIncludeArrayAndExtensionDeclarations() {
        PhpFile file = (PhpFile) myFixture.addFileToProject("config/packages/ux.php", """
            <?php
            $container->extension('ux_css', ['tokens' => []]);
            $container->extension('framework', ['tokens' => []]);
            $unrelated = ['ux_css' => ['tokens' => []]];
            return App::config(['ux_css' => ['semantic_tokens' => []]]);
            """);

        assertSize(2, UxCssConfigUtil.getPhpConfigs(file));
        assertTrue(UxCssConfigUtil.getPhpConfigs(file).stream().anyMatch(config -> "['tokens' => []]".equals(config.getText())));
        assertTrue(UxCssConfigUtil.getPhpConfigs(file).stream().anyMatch(config -> "['semantic_tokens' => []]".equals(config.getText())));
    }
}
