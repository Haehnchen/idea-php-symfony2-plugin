package fr.adrienbrault.idea.symfony2plugin.tests.routing.annotation;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.routing.annotation.RouteUrlAnnotationCompletionProvider
 */
public class RouteUrlAnnotationCompletionProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
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
                "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
                "\n" +
                "class FooController\n" +
                "{\n" +
                "    /**\n" +
                "     * @Route(\"<caret>\")\n" +
                "     */\n" +
                "    public function foo1() {}\n" +
                "}",
            "/bar"
        );

        assertCompletionContains("test.php", "<?php\n" +
                "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
                "\n" +
                "class FooController\n" +
                "{\n" +
                "    /**\n" +
                "     * @Route(path=\"<caret>\")\n" +
                "     */\n" +
                "    public function foo1() {}\n" +
                "}",
            "/bar"
        );
    }


    public void testRouteUrlCompletionForAttribute() {
        // some ast node change that break annotation plugin
        if (false) {
            return;
        }

        assertCompletionContains("test.php", "<?php\n" +
                "\n" +
                "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
                "\n" +
                "#[Route(\"<caret>\")]\n" +
                "{\n" +
                "    /**\n" +
                "     * @Route(\"<caret>\")\n" +
                "     */\n" +
                "    public function foo1() {}\n" +
                "}",
            "/bar"
        );

        assertCompletionContains("test.php", "<?php\n" +
                "\n" +
                "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
                "\n" +
                "#[Route(path: \"<caret>\")]\n" +
                "{\n" +
                "    /**\n" +
                "     * @Route(\"<caret>\")\n" +
                "     */\n" +
                "    public function foo1() {}\n" +
                "}",
            "/bar"
        );
    }
}
