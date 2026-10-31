package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.UxCssConfigStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @see UxCssConfigStubIndex
 */
public class UxCssConfigStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testYamlConfigIsIndexed() {
        myFixture.addFileToProject("config/packages/ux.yaml", "ux_css:\n    tokens: ~\n");

        assertIndexContains(UxCssConfigStubIndex.KEY, "ux_css");
    }

    public void testConditionalYamlConfigIsIndexed() {
        myFixture.addFileToProject("config/packages/ux.yaml", "when@dev:\n    ux_css:\n        tokens: ~\n");

        assertIndexContains(UxCssConfigStubIndex.KEY, "ux_css");
    }

    public void testPhpConfigIsIndexed() {
        myFixture.addFileToProject("config/packages/ux.php", "<?php\nreturn App::config(['ux_css' => ['tokens' => []]]);\n");

        assertIndexContains(UxCssConfigStubIndex.KEY, "ux_css");
    }

    public void testPhpExtensionConfigIsIndexed() {
        myFixture.addFileToProject("config/packages/ux.php", "<?php\nreturn static function ($container) {\n    $container->extension('ux_css', ['tokens' => []]);\n};\n");

        assertIndexContains(UxCssConfigStubIndex.KEY, "ux_css");
    }

    public void testFilesWithoutConfigAreNotIndexed() {
        myFixture.addFileToProject("config/packages/framework.yaml", "framework:\n    ux_css: ~\n");
        myFixture.addFileToProject("src/Foo.php", "<?php\n$foo = ['ux_css' => []];\n");

        assertIndexNotContains(UxCssConfigStubIndex.KEY, "ux_css");
    }
}
