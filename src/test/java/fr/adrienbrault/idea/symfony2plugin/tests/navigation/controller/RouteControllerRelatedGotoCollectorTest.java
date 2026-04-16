package fr.adrienbrault.idea.symfony2plugin.tests.navigation.controller;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.navigation.controller.RouteControllerRelatedGotoCollector
 */
public class RouteControllerRelatedGotoCollectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("routing.yml"));
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/navigation/controller/fixtures";
    }

    public void testThatControllerProvidesYamDefinitionNavigation() {
        assertLineMarker(myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Foo\\Route {\n" +
            "   class Bar {\n" +
            "       function fooAction() {}\n" +
            "   }\n" +
            "}"
        ), new LineMarker.ToolTipEqualsAssert("foo_route_bar_foo"));

        assertLineMarker(myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Foo\\Route {\n" +
            "   class Bar {\n" +
            "       function barAction() {}\n" +
            "   }\n" +
            "}"
        ), new LineMarker.ToolTipEqualsAssert("foo_route_bar_bar_sequence"));
    }

    public void testThatControllerProvidesPhpRoutingDefinitionNavigation() {
        myFixture.addFileToProject("routing/classes.php", "<?php\n" +
            "namespace Symfony\\Component\\Routing\\Loader\\Configurator\n" +
            "{\n" +
            "    use Symfony\\Component\\Routing\\Loader\\Configurator\\Traits\\AddTrait;\n" +
            "    use Symfony\\Component\\Routing\\Loader\\Configurator\\Traits\\RouteTrait;\n" +
            "\n" +
            "    class RouteConfigurator\n" +
            "    {\n" +
            "        use AddTrait;\n" +
            "        use RouteTrait;\n" +
            "    }\n" +
            "\n" +
            "    class RoutingConfigurator\n" +
            "    {\n" +
            "        use AddTrait;\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "namespace Symfony\\Component\\Routing\\Loader\\Configurator\\Traits\n" +
            "{\n" +
            "    use Symfony\\Component\\Routing\\Loader\\Configurator\\RouteConfigurator;\n" +
            "\n" +
            "    trait RouteTrait\n" +
            "    {\n" +
            "        final public function controller(callable|string|array $controller): static {}\n" +
            "    }\n" +
            "\n" +
            "    trait AddTrait\n" +
            "    {\n" +
            "        public function add(string $name, string|array $path): RouteConfigurator {}\n" +
            "    }\n" +
            "}\n");

        myFixture.addFileToProject("config/routes.php", "<?php\n" +
            "use Symfony\\Component\\Routing\\Loader\\Configurator\\RoutingConfigurator;\n" +
            "\n" +
            "return static function (RoutingConfigurator $routes): void {\n" +
            "    $route = $routes->add('_profiler_home', '/');\n" +
            "    $route->controller('web_profiler.controller.profiler::homeAction');\n" +
            "};");

        myFixture.addFileToProject("config/services.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<container>\n" +
            "    <services>\n" +
            "        <service id=\"web_profiler.controller.profiler\" class=\"App\\Controller\\ProfilerController\"/>\n" +
            "    </services>\n" +
            "</container>\n");

        assertLineMarker(myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;\n" +
            "class ProfilerController {\n" +
            "   public function homeAction() {}\n" +
            "}\n"
        ), new LineMarker.ToolTipEqualsAssert("_profiler_home"));
    }

}
