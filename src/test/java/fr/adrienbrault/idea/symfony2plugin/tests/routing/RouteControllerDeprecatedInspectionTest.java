package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.routing.RouteControllerDeprecatedInspection
 */
public class RouteControllerDeprecatedInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("RouteControllerDeprecatedInspection.php");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/fixtures";
    }

    public void testDeprecatedRouteActionForYml() {
        assertLocalInspectionContains("foobar.yml","" +
                "blog_list:\n" +
                "    controller: App\\Controller\\BarController::foo<caret>bar",
            "Symfony: Controller action is deprecated"
        );

        assertLocalInspectionContains("foobar.yml","" +
                "blog_list:\n" +
                "    defaults: { _controller: App\\Controller\\BarController::foo<caret>bar }",
            "Symfony: Controller action is deprecated"
        );
    }

    public void testDeprecatedRouteActionForXml() {
        assertLocalInspectionContains("foobar.xml",
            "<routes>\n" +
                "   <route controller=\"App\\Controller\\BarController::foo<caret>bar\"/>\n" +
                "</routes>",
            "Symfony: Controller action is deprecated"
        );

        assertLocalInspectionContains("foobar.xml",
            "<routes>\n" +
                "   <route>\n" +
                "       <default key=\"_controller\">App\\Controller\\BarController::foo<caret>bar</default>\n" +
                "   </route>\n" +
                "</routes>",
            "Symfony: Controller action is deprecated"
        );
    }

    public void testDeprecatedRouteActionForClassMember() {
        assertLocalInspectionContains("foobar.yml","" +
                "blog_list:\n" +
                "    controller: App\\Controller\\CarController::foo<caret>bar",
            "Symfony: Controller action is deprecated"
        );
    }
}