package fr.adrienbrault.idea.symfony2plugin.tests.routing.annotation;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.routing.annotation.RouteNameAnnotationCompletionProvider
 */
public class RouteNameAnnotationCompletionProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("routing.yml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/annotation/fixtures";
    }

    public void testRouteUrlCompletionForAnnotations() {
        assertCompletionContains("test.php", "<?php\n" +
                "namespace App\\Controller;" +
                "" +
                "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
                "\n" +
                "class FooController\n" +
                "{\n" +
                "    /**\n" +
                "     * @Route(name=\"<caret>\")\n" +
                "     */\n" +
                "    public function foo1() {}\n" +
                "}",
            "app_foo_foo1"
        );
    }

    public void testRouteUrlCompletionForAttributes() {
        assertCompletionContains("test.php", "<?php\n" +
                "namespace App\\Controller;" +
                "" +
                "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
                "\n" +
                "class FooController\n" +
                "{\n" +
                "    #[Route(name: '<caret>']" +
                "    public function foo1() {}\n" +
                "}",
            "app_foo_foo1"
        );
    }
}
