package fr.adrienbrault.idea.symfony2plugin.tests.routing.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.routing.inspection.ControllerMethodInspection
 */
public class ControllerMethodInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("services.yml");
        myFixture.copyFileToProject("classes.php");
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testClassControllerMethodNotFoundProvidesWarning() {
        assertLocalInspectionContains("routing.yml", "" +
                "foo:\n" +
                "    defaults: { _controller: Route\\Controller\\FooController::barA<caret>ction }",
            "Create Method"
        );

        assertLocalInspectionContains("routing.yml", "" +
                "foo:\n" +
                "    defaults:\n" +
                "      _controller: Route\\Controller\\FooController::barA<caret>ction",
            "Create Method"
        );

        assertLocalInspectionNotContains("routing.yml", "" +
            "foo:\n" +
            "    defaults: { _controller: Route\\Controller\\FooController::fooA<caret>ction }",
            "Create Method"
        );
    }

    public void testClassControllerAsServiceWithClassNameAsServiceId() {
        assertLocalInspectionNotContains("routing.yml", "" +
                "foo:\n" +
                "    defaults:\n" +
                "      _controller: Route\\Controller\\FooController:foo<caret>Action",
            "Create Method"
        );

        assertLocalInspectionContains("routing.yml", "" +
                "foo:\n" +
                "    defaults:\n" +
                "      _controller: Route\\Controller\\FooController:bar<caret>Action",
            "Create Method"
        );
    }
}
