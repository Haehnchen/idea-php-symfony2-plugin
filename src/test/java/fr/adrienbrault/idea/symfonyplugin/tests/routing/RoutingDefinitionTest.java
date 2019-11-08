package fr.adrienbrault.idea.symfonyplugin.tests.routing;

import com.intellij.ide.highlighter.XmlFileType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.util.controller.ControllerIndex
 */
public class RoutingDefinitionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("services.yml", "services.yml");
        myFixture.copyFileToProject("routing.xml", "routing.xml");

        myFixture.configureByText("classes.php", "<?php\n" +
                "namespace AppBundle;\n" +
                "class AppBundle extends \\Symfony\\Component\\HttpKernel\\Bundle\\Bundle {}" +

                "namespace AppBundle\\Controller;\n" +
                "class DefaultController {\n" +
                "    public function indexAction() {}\n" +
                "    private function fooAction() {}\n" +
                "    public function indexActionFoo() {}\n" +
                "}" +

                "namespace AppBundle\\Controller\\Foo;\n" +
                "class DefaultController {\n" +
                "    public function indexAction() {}\n" +
                "}"
        );
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/routing/fixtures";
    }

    public void testYamlCompletion() {

        assertCompletionContains(YAMLFileType.YML, "foo:\n" +
                "    pattern: /\n" +
                "    defaults: { _controller: <caret> }\n"
            , "AppBundle:Default:index", "AppBundle:Foo\\Default:index"
        );

        assertCompletionNotContains(YAMLFileType.YML, "foo:\n" +
                "    pattern: /\n" +
                "    defaults: { _controller: <caret> }\n"
            , "AppBundle:Default:foo"
        );

        assertCompletionContains(YAMLFileType.YML, "foo:\n" +
                "    pattern: /\n" +
                "    defaults: { _controller: '<caret>' }\n"
            , "AppBundle:Default:index", "AppBundle:Foo\\Default:index"
        );

        assertCompletionNotContains(YAMLFileType.YML, "foo:\n" +
                "    pattern: /\n" +
                "    defaults: { _controller: \"<caret>\" }\n"
            , "AppBundle:Default:foo"
        );

    }

    public void testYamlNavigation() {

        assertNavigationContains(YAMLFileType.YML, "foo:\n" +
                "    pattern: /\n" +
                "    defaults: { _controller: AppBundle:<caret>Default:index }\n"
            , "AppBundle\\Controller\\DefaultController::indexAction"
        );

        assertNavigationContains(YAMLFileType.YML, "foo:\n" +
                "    pattern: /\n" +
                "    defaults: { _controller: AppBundle:Foo\\<caret>Default:index }\n"
            , "AppBundle\\Controller\\Foo\\DefaultController::indexAction"
        );

    }

    public void testXmlCompletion() {

        assertCompletionContains(XmlFileType.INSTANCE, "" +
                "<route id=\"blog_show\" path=\"/blog/{slug}\">\n" +
                "    <default key=\"_controller\"><caret></default>\n" +
                "</route>"
            , "AppBundle:Default:index", "AppBundle:Foo\\Default:index"
        );

    }

    public void testControllerAsServiceNavigation() {

        assertNavigationContains(YAMLFileType.YML, "foo:\n" +
                "    pattern: /\n" +
                "    defaults: { _controller: app.hello_controller<caret>:indexAction }\n"
            , "AppBundle\\Controller\\DefaultController::indexAction"
        );

    }

    public void testControllerAsServiceCompletion() {

        assertCompletionContains(YAMLFileType.YML, "foo:\n" +
                "    pattern: /\n" +
                "    defaults: { _controller: app.hello_controller:<caret> }\n"
            , "app.hello_controller:indexAction"
        );

    }

}
