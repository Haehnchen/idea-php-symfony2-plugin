package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection

import fr.adrienbrault.idea.symfony2plugin.templating.inspection.PhpAssetMissingInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.inspection.PhpAssetMissingInspection
 */
class PhpAssetMissingInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        myFixture.copyFileToProject("classes.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/templating/inspection/fixtures"
    }

    fun testThatUnknownAssetIsHighlighted() {
        assertLocalInspectionContains(PhpAssetMissingInspection::class.java,
            "test.php",
            "<?php\n" +
                "(new \\Symfony\\Component\\Asset\\Packages())->getVersion('foob<caret>ar.css');\n",
            "Symfony: Missing asset"
        )

        assertLocalInspectionContains(PhpAssetMissingInspection::class.java,
            "test.php",
            "<?php\n" +
                "(new \\Symfony\\Component\\Asset\\Package())->getVersion('foob<caret>ar.css');\n",
            "Symfony: Missing asset"
        )

        assertLocalInspectionContains(PhpAssetMissingInspection::class.java,
            "test.php",
            "<?php\n" +
                "(new \\Symfony\\Component\\Asset\\Packages())->getUrl('foob<caret>ar.css');\n",
            "Symfony: Missing asset"
        )

        assertLocalInspectionContains(PhpAssetMissingInspection::class.java,
            "test.php",
            "<?php\n" +
                "(new \\Symfony\\Component\\Asset\\Package())->getUrl('foob<caret>ar.css');\n",
            "Symfony: Missing asset"
        )
    }
}
