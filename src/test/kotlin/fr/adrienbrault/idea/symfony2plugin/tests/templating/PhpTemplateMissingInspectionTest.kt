package fr.adrienbrault.idea.symfony2plugin.tests.templating

import fr.adrienbrault.idea.symfony2plugin.templating.PhpTemplateMissingInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.PhpTemplateMissingInspection
 */
class PhpTemplateMissingInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("TwigTemplateMissingInspection.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures"
    }

    fun testThatInspectionIsAvailable() {
        assertLocalInspectionContains(PhpTemplateMissingInspection::class.java, "test.php", "<?php\n" +
            "/** @var \$x \\Symfony\\Component\\Templating\\EngineInterface */" +
            "\$x->render('<caret>test.html.twig')",
            "Twig: Missing Template"
        )
    }
}
