package fr.adrienbrault.idea.symfony2plugin.tests.routing.inspection

import fr.adrienbrault.idea.symfony2plugin.routing.inspection.PhpRouteMissingInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.routing.inspection.PhpRouteMissingInspection
 */
class PhpRouteMissingInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        myFixture.copyFileToProject("PhpRouteMissingInspection.php")
        myFixture.copyFileToProject("PhpRouteMissingInspection.xml")
        myFixture.copyFileToProject("RouteDeprecatedInspection.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/inspection/fixtures"
    }

    fun testRouteDoesNotExistsInspection() {
        assertLocalInspectionContains(PhpRouteMissingInspection::class.java, "test.php", "<?php\n" +
            "/** @var \$x \\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface */\n" +
            "\$x->generate('fo<caret>obar');\n",
            "Symfony: Missing Route"
        )
    }

    fun testRouteDoesNotExistsInspectionMustNotBeShownForExistingRoute() {
        assertLocalInspectionNotContains(PhpRouteMissingInspection::class.java, "test.php", "<?php\n" +
            "/** @var \$x \\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface */\n" +
            "\$x->generate('my_fo<caret>obar');\n",
            "Symfony: Missing Route"
        )
    }

    fun testRouteUsageForDeprecatedControllerActionProvidesInspection() {
        assertLocalInspectionContains(PhpRouteMissingInspection::class.java, "test.php", "<?php\n" +
            "/** @var \$x \\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface */\n" +
            "\$x->generate('deprecated_<caret>route');\n",
            "Symfony: Controller action is deprecated"
        )

        assertLocalInspectionNotContains(PhpRouteMissingInspection::class.java, "test.php", "<?php\n" +
            "/** @var \$x \\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface */\n" +
            "\$x->generate('active_<caret>route');\n",
            "Symfony: Controller action is deprecated"
        )
    }
}
