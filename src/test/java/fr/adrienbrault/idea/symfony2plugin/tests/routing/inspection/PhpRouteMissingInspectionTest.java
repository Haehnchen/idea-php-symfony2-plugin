package fr.adrienbrault.idea.symfony2plugin.tests.routing.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.routing.inspection.PhpRouteMissingInspection
 */
public class PhpRouteMissingInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("PhpRouteMissingInspection.php");
        myFixture.copyFileToProject("PhpRouteMissingInspection.xml");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/inspection/fixtures";
    }

    public void testRouteDoesNotExistsInspection() {
        assertLocalInspectionContains("test.php", "<?php\n" +
                "/** @var $x \\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface */\n" +
                "$x->generate('fo<caret>obar');\n",
            "Missing Route"
        );
    }

    public void testRouteDoesNotExistsInspectionMustNotBeShownForExistingRoute() {
        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "/** @var $x \\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface */\n" +
                "$x->generate('my_fo<caret>obar');\n",
            "Missing Route"
        );
    }
}
