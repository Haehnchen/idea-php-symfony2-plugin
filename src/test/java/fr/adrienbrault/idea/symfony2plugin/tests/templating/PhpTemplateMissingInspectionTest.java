package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.PhpTemplateMissingInspection
 */
public class PhpTemplateMissingInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("TwigTemplateMissingInspection.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    public void testThatInspectionIsAvailable() {
        assertLocalInspectionContains("test.php", "<?php" +
                "<?php\n" +
                "/** @var $x \\Symfony\\Component\\Templating\\EngineInterface */" +
                "$x->render('<caret>test.html.twig')",
            "Twig: Missing Template"
        );
    }
}
