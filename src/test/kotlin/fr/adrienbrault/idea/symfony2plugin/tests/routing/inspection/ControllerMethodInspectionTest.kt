package fr.adrienbrault.idea.symfony2plugin.tests.routing.inspection

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.routing.inspection.ControllerMethodInspection
 */
class ControllerMethodInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        myFixture.copyFileToProject("services.yml")
        myFixture.copyFileToProject("classes.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/inspection/fixtures"
    }

    fun testYamlClassControllerMethodNotFoundProvidesWarning() {
        assertLocalInspectionContains("routing.yml", "" +
            "foo:\n" +
            "    defaults: { _controller: Route\\Controller\\FooController::barA<caret>ction }",
            "Create Method"
        )

        assertLocalInspectionContains("routing.yml", "" +
            "foo:\n" +
            "    defaults:\n" +
            "      _controller: Route\\Controller\\FooController::barA<caret>ction",
            "Create Method"
        )

        assertLocalInspectionNotContains("routing.yml", "" +
            "foo:\n" +
            "    defaults: { _controller: Route\\Controller\\FooController::fooA<caret>ction }",
            "Create Method"
        )

        assertLocalInspectionNotContains("routing.yml", "" +
            "foo:\n" +
            "    controller: Route\\Controller\\FooController::fooA<caret>ction\n",
            "Create Method"
        )
    }

    fun testYamlClassControllerAsServiceWithClassNameAsServiceId() {
        assertLocalInspectionNotContains("routing.yml", "" +
            "foo:\n" +
            "    defaults:\n" +
            "      _controller: Route\\Controller\\FooController:foo<caret>Action",
            "Create Method"
        )

        assertLocalInspectionContains("routing.yml", "" +
            "foo:\n" +
            "    defaults:\n" +
            "      _controller: Route\\Controller\\FooController:bar<caret>Action",
            "Create Method"
        )

        assertLocalInspectionContains("routing.yml", "" +
            "foo:\n" +
            "    controller: Route\\Controller\\FooController:bar<caret>Action",
            "Create Method"
        )
    }

    fun testXmlClassControllerMethodNotFoundProvidesWarning() {
        assertLocalInspectionContains("routing.xml", "" +
            "<routes>\n" +
            "    <route id=\"blog_list\" path=\"/blog\">\n" +
            "        <default key=\"_controller\">Route\\Controller\\FooController:bar<caret>Action</default>\n" +
            "    </route>\n" +
            "</routes>\n",
            "Create Method"
        )

        assertLocalInspectionNotContains("routing.yml", "" +
            "<routes>\n" +
            "    <route id=\"blog_list\" path=\"/blog\">\n" +
            "        <default key=\"_controller\">Route\\Controller\\FooController::fooA<caret>ction</default>\n" +
            "    </route>\n" +
            "</routes>\n",
            "Create Method"
        )
    }

    fun testXmlClassControllerMethodNotFoundProvidesWarningForControllerKeyword() {
        assertLocalInspectionContains("routing.xml", "" +
            "<routes>\n" +
            "    <route id=\"blog_list\" controller=\"Route\\Controller\\FooController:bar<caret>Action\"/>\n" +
            "</routes>\n",
            "Create Method"
        )
    }
}
