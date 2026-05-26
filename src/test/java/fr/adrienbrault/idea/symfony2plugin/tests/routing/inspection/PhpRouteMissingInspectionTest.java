package fr.adrienbrault.idea.symfony2plugin.tests.routing.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.routing.inspection.PhpRouteMissingInspection
 */
public class PhpRouteMissingInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("PhpRouteMissingInspection.php");
        myFixture.copyFileToProject("PhpRouteMissingInspection.xml");
        myFixture.copyFileToProject("RouteDeprecatedInspection.php");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/inspection/fixtures";
    }

    public void testRouteDoesNotExistsInspection() {
        assertLocalInspectionContains("test.php", "<?php\n" +
                "/** @var $x \\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface */\n" +
                "$x->generate('fo<caret>obar');\n",
            "Symfony: Missing Route"
        );
    }

    public void testRouteDoesNotExistsInspectionMustNotBeShownForExistingRoute() {
        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "/** @var $x \\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface */\n" +
                "$x->generate('my_fo<caret>obar');\n",
            "Symfony: Missing Route"
        );
    }

    public void testRouteUsageForDeprecatedControllerActionProvidesInspection() {
        assertLocalInspectionContains("test.php", "<?php\n" +
                "/** @var $x \\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface */\n" +
                "$x->generate('deprecated_<caret>route');\n",
            "Symfony: Controller action is deprecated"
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "/** @var $x \\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface */\n" +
                "$x->generate('active_<caret>route');\n",
            "Symfony: Controller action is deprecated"
        );
    }
}
