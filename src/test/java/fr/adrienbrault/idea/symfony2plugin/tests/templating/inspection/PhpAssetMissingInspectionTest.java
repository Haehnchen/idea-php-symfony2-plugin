package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.inspection.PhpAssetMissingInspection
 */
public class PhpAssetMissingInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/inspection/fixtures";
    }

    public void testThatUnknownAssetIsHighlighted() {
        assertLocalInspectionContains(
            "test.php",
            "<?php\n" +
                "(new \\Symfony\\Component\\Asset\\Packages())->getVersion('foob<caret>ar.css');\n",
            "Symfony: Missing asset"
        );

        assertLocalInspectionContains(
            "test.php",
            "<?php\n" +
                "(new \\Symfony\\Component\\Asset\\Package())->getVersion('foob<caret>ar.css');\n",
            "Symfony: Missing asset"
        );

        assertLocalInspectionContains(
            "test.php",
            "<?php\n" +
                "(new \\Symfony\\Component\\Asset\\Packages())->getUrl('foob<caret>ar.css');\n",
            "Symfony: Missing asset"
        );

        assertLocalInspectionContains(
            "test.php",
            "<?php\n" +
                "(new \\Symfony\\Component\\Asset\\Package())->getUrl('foob<caret>ar.css');\n",
            "Symfony: Missing asset"
        );
    }
}
