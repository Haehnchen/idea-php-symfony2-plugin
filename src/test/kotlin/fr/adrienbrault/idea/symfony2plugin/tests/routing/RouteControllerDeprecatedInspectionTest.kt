package fr.adrienbrault.idea.symfony2plugin.tests.routing

import fr.adrienbrault.idea.symfony2plugin.routing.RouteControllerDeprecatedInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.routing.RouteControllerDeprecatedInspection
 */
class RouteControllerDeprecatedInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        myFixture.copyFileToProject("RouteControllerDeprecatedInspection.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/routing/fixtures"
    }

    fun testDeprecatedRouteActionForYml() {
        assertLocalInspectionContains(RouteControllerDeprecatedInspection.RouteControllerDeprecatedYamlLocalInspectionTool::class.java, "foobar.yml", "" +
            "blog_list:\n" +
            "    controller: App\\Controller\\BarController::foo<caret>bar",
            "Symfony: Controller action is deprecated"
        )

        assertLocalInspectionContains(RouteControllerDeprecatedInspection.RouteControllerDeprecatedYamlLocalInspectionTool::class.java, "foobar.yml", "" +
            "blog_list:\n" +
            "    defaults: { _controller: App\\Controller\\BarController::foo<caret>bar }",
            "Symfony: Controller action is deprecated"
        )
    }

    fun testDeprecatedRouteActionForXml() {
        assertLocalInspectionContains(RouteControllerDeprecatedInspection.RouteControllerDeprecatedXmlLocalInspectionTool::class.java, "foobar.xml",
            "<routes>\n" +
                "   <route controller=\"App\\Controller\\BarController::foo<caret>bar\"/>\n" +
                "</routes>",
            "Symfony: Controller action is deprecated"
        )

        assertLocalInspectionContains(RouteControllerDeprecatedInspection.RouteControllerDeprecatedXmlLocalInspectionTool::class.java, "foobar.xml",
            "<routes>\n" +
                "   <route>\n" +
                "       <default key=\"_controller\">App\\Controller\\BarController::foo<caret>bar</default>\n" +
                "   </route>\n" +
                "</routes>",
            "Symfony: Controller action is deprecated"
        )
    }

    fun testDeprecatedRouteActionForClassMember() {
        assertLocalInspectionContains(RouteControllerDeprecatedInspection.RouteControllerDeprecatedYamlLocalInspectionTool::class.java, "foobar.yml", "" +
            "blog_list:\n" +
            "    controller: App\\Controller\\CarController::foo<caret>bar",
            "Symfony: Controller action is deprecated"
        )
    }

    fun testDeprecatedRouteActionForPhpAttribute() {
        // Test deprecated method with #[\Deprecated] attribute
        assertLocalInspectionContains(RouteControllerDeprecatedInspection.RouteControllerDeprecatedYamlLocalInspectionTool::class.java, "foobar.yml", "" +
            "blog_deprecated_method:\n" +
            "    controller: App\\Controller\\DeprecatedAttributeController::newDeprecatedMeth<caret>od",
            "Symfony: Controller action is deprecated"
        )

        // Test deprecated method with #[\Deprecated] attribute and message
        assertLocalInspectionContains(RouteControllerDeprecatedInspection.RouteControllerDeprecatedYamlLocalInspectionTool::class.java, "foobar.yml", "" +
            "blog_deprecated_method_message:\n" +
            "    controller: App\\Controller\\DeprecatedAttributeController::newDeprecatedMethodWithMessa<caret>ge",
            "Symfony: Controller action is deprecated"
        )

        // Test non-deprecated method should not trigger inspection
        assertLocalInspectionNotContains(RouteControllerDeprecatedInspection.RouteControllerDeprecatedYamlLocalInspectionTool::class.java, "foobar.yml", "" +
            "blog_not_deprecated:\n" +
            "    controller: App\\Controller\\DeprecatedAttributeController::notDeprecatedMet<caret>hod",
            "Symfony: Controller action is deprecated"
        )
    }

    fun testDeprecatedRouteActionForDeprecatedClassWithAttribute() {
        // Test deprecated class with #[\Deprecated] attribute
        assertLocalInspectionContains(RouteControllerDeprecatedInspection.RouteControllerDeprecatedYamlLocalInspectionTool::class.java, "foobar.yml", "" +
            "blog_deprecated_class:\n" +
            "    controller: App\\Controller\\DeprecatedClassController::someMeth<caret>od",
            "Symfony: Controller action is deprecated"
        )

        // Test deprecated class with #[\Deprecated] attribute and message
        assertLocalInspectionContains(RouteControllerDeprecatedInspection.RouteControllerDeprecatedYamlLocalInspectionTool::class.java, "foobar.yml", "" +
            "blog_deprecated_class_message:\n" +
            "    controller: App\\Controller\\DeprecatedClassWithMessageController::someMeth<caret>od",
            "Symfony: Controller action is deprecated"
        )
    }

    fun testDeprecatedRouteActionForPhpAttributeXml() {
        // Test deprecated method with #[\Deprecated] attribute in XML
        assertLocalInspectionContains(RouteControllerDeprecatedInspection.RouteControllerDeprecatedXmlLocalInspectionTool::class.java, "foobar.xml",
            "<routes>\n" +
                "   <route controller=\"App\\Controller\\DeprecatedAttributeController::newDeprecatedMeth<caret>od\"/>\n" +
                "</routes>",
            "Symfony: Controller action is deprecated"
        )

        // Test deprecated class with #[\Deprecated] attribute in XML
        assertLocalInspectionContains(RouteControllerDeprecatedInspection.RouteControllerDeprecatedXmlLocalInspectionTool::class.java, "foobar.xml",
            "<routes>\n" +
                "   <route>\n" +
                "       <default key=\"_controller\">App\\Controller\\DeprecatedClassController::someMeth<caret>od</default>\n" +
                "   </route>\n" +
                "</routes>",
            "Symfony: Controller action is deprecated"
        )
    }
}
