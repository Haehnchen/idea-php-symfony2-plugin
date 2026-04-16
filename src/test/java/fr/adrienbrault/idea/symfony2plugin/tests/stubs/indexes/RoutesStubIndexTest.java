package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.routing.dict.RouteInterface;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex
 */
public class RoutesStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("RoutesStubIndex.php");
        myFixture.copyFileToProject("RoutesStubIndex.yml");
        myFixture.copyFileToProject("RoutesStubIndex.xml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/indexes/fixtures";
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex#getIndexer()
     */
    public void testRouteIdIndex() {
        assertIndexContains(RoutesStubIndex.KEY,
            "foo_yaml_pattern", "foo_yaml_path", "foo_yaml_path_only",
            "foo_xml_pattern", "foo_xml_path", "foo_xml_id_only", "attributes_action", "app_my_default_attributeswithoutname",
            "my_post_emptyannotation", "myattributesprefix_prefixdefaultparameter_emptyattribute", "multiple_attributes"
        );

        assertIndexNotContains(RoutesStubIndex.KEY,
            "foo_yaml_invalid"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex#getIndexer()
     */
    public void testRouteValueIndex() {
        assertIndexContainsKeyWithValue(RoutesStubIndex.KEY, "foo_yaml_path",
            value -> "foo_controller".equalsIgnoreCase(value.getController())
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex#getIndexer()
     */
    public void testRouteValueWithMethodsInIndex() {
        assertIndexContainsKeyWithValue(RoutesStubIndex.KEY, "foo_yaml_pattern",
            value -> "foo_yaml_pattern".equalsIgnoreCase(value.getName()) && value.getMethods().contains("get") && value.getMethods().contains("post")
        );

        assertIndexContainsKeyWithValue(RoutesStubIndex.KEY, "foo_xml_pattern",
            value -> "foo_xml_pattern".equalsIgnoreCase(value.getName()) && value.getMethods().contains("get") && value.getMethods().contains("post")
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex#getIndexer()
     */
    public void testRouteSlashesNormalized() {
        assertIndexContainsKeyWithValue(RoutesStubIndex.KEY, "foo_yaml_controller_normalized",
            value -> "FooBundle:Foo\\Foo:index".equalsIgnoreCase(value.getController())
        );

        assertIndexContainsKeyWithValue(RoutesStubIndex.KEY, "foo_controller_normalized",
            value -> "FooBundle:Foo\\Foo:index".equalsIgnoreCase(value.getController())
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex#getIndexer()
     */
    public void testControllerAsInvokeRoutingSupportsClassPatternForDefaults() {
        myFixture.configureByText(YAMLFileType.YML, "" +
            "controller_invoke:\n" +
            "    pattern: /\n" +
            "    defaults: { _controller: Foobar\\Foobar\\Foobar }" +
            "\n" +
            "controller_invoke_2:\n" +
            "    pattern: /\n" +
            "    defaults: { _controller: \\Foobar\\Foobar\\Foobar }" +
            "\n"
        );

        assertIndexContainsKeyWithValue(RoutesStubIndex.KEY, "controller_invoke",
            value -> "Foobar\\Foobar\\Foobar".equalsIgnoreCase(value.getController())
        );

        assertIndexContainsKeyWithValue(RoutesStubIndex.KEY, "controller_invoke_2",
            value -> "Foobar\\Foobar\\Foobar".equalsIgnoreCase(value.getController())
        );
    }


    public void testAnnotationIndexOfMethodPatternAndClassPrefix() {
        assertIndexContains(RoutesStubIndex.KEY, "blog_home");
        RouteInterface route = getFirstValue("blog_home");

        assertEquals("blog_home", route.getName());
        assertEquals("/foo/edit/{id}", route.getPath());
        assertEquals("My\\PostController::editAction", route.getController());
    }

    public void testAnnotationIndexOfMethodPatternAndClassPrefixWithSpecialPath() {
        assertIndexContains(RoutesStubIndex.KEY, "blog_home_special");
        RouteInterface route = getFirstValue("blog_home_special");

        assertEquals("blog_home_special", route.getName());
        assertEquals("/foo/edit/{!id<.*>}/{!id<\\d+>}////", route.getPath());

        RouteInterface route1 = getFirstValue("blog_home_the_special_placeholder_one");
        assertEquals("/blog/{page<\\d+>?1}/{page1<\\d+>?}/{page2<\\d+>?}/{parameter_name?default_value}", route1.getPath());
    }

    public void testAnnotationThatEmptyRouteNameUseBundleMethodName() {
        assertIndexContains(RoutesStubIndex.KEY, "myfoobar_car_index");
        RouteInterface route = getFirstValue("myfoobar_car_index");

        assertEquals("myfoobar_car_index", route.getName());
        assertEquals("/foo_bar/edit/{id}", route.getPath());
        assertEquals("MyFooBarBundle\\Controller\\CarController::indexAction", route.getController());
    }

    public void testAnnotationThatEmptyRouteNameUseBundleMethodNameAndStripsReservedWords() {
        assertIndexContains(RoutesStubIndex.KEY, "foo_parkresort_sub_bundle_foo_nestedfoo");
        assertIndexContains(RoutesStubIndex.KEY, "foo_parkresort_sub_car_index");
        assertIndexContains(RoutesStubIndex.KEY, "foo_parkresort_default_index");
        assertIndexContains(RoutesStubIndex.KEY, "foo_parkresort_actions_foo_index");
        assertIndexContains(RoutesStubIndex.KEY, "app_default_foo");
    }

    public void testAnnotationThatMethodsAreInIndex() {
        RouteInterface route = getFirstValue("blog_home");
        route.getMethods().contains("get");

        route = getFirstValue("blog_home_get_head");
        route.getMethods().contains("get");
        route.getMethods().contains("head");
    }

    public void testAnnotationThatRouteOfComponentRoutingAnnotationRouteIsIndexed() {
        assertIndexContains(RoutesStubIndex.KEY, "framework_extra_bundle_route");
    }

    public void testAnnotationThatRouteWithInvokeMustNotAddAdditionalUnderscore() {
        assertIndexContains(RoutesStubIndex.KEY, "my_post__invoke");
    }

    public void testAnnotationThatRouteWithPrefixIsInIndex() {
        assertIndexContains(RoutesStubIndex.KEY, "foo_prefix_home");
    }

    public void testThatPhp8AttributesMethodsAreInIndex() {
        RouteInterface route = getFirstValue("attributes_action");
        assertEquals("attributes_action", route.getName());
        assertEquals("AppBundle\\My\\Controller\\DefaultController::attributesAction", route.getController());
        assertEquals("/attributes-action", route.getPath());

        RouteInterface route2 = getFirstValue("app_my_default_attributeswithoutname");
        assertEquals("app_my_default_attributeswithoutname", route2.getName());
        assertEquals("AppBundle\\My\\Controller\\DefaultController::attributesWithoutName", route2.getController());
        assertEquals("/attributesWithoutName", route2.getPath());
        assertContainsElements(route2.getMethods(), "POST", "GET");

        RouteInterface route3 = getFirstValue("attributes-names");
        assertEquals("attributes-names", route3.getName());
        assertEquals("AppBundle\\My\\Controller\\DefaultController::attributesPath", route3.getController());
        assertEquals("/attributes-path", route3.getPath());

        RouteInterface route4 = getFirstValue("foo-attributes_prefix_home");
        assertEquals("MyAttributesPrefix\\PrefixController::editAction", route4.getController());
        assertEquals("foo-attributes_prefix_home", route4.getName());
        assertEquals("/foo-attributes/edit/{id}", route4.getPath());

        RouteInterface route5 = getFirstValue("prefix_home_default_parameter");
        assertEquals("/foo-attributes-default/edit/{id}", route5.getPath());

        RouteInterface route6 = getFirstValue("attributes-names-foobar-const");
        assertEquals("/attributes-path-foobar", route6.getPath());

        RouteInterface route7 = getFirstValue("attributes-default-as-const");
        assertEquals("/attributes-path-foobar", route7.getPath());

        RouteInterface route8 = getFirstValue("attributesWithoutNameWithConstantsInMethods");
        assertEquals(0, route8.getMethods().size());

        RouteInterface route9 = getFirstValue("foo-attributes-not-named_prefix_home_not_named");
        assertEquals("MyAttributesPrefix\\PrefixNotNamedController::editAction", route9.getController());
        assertEquals("foo-attributes-not-named_prefix_home_not_named", route9.getName());
        assertEquals("/foo-attributes/edit-not-named/{id}", route9.getPath());
    }

    public void testThatPhp8AttributesViaClassInvokeMethodsAreInIndex() {
        RouteInterface route = getFirstValue("invoke_route_attribute");
        assertEquals("/foo-attributes", route.getPath());
        assertEquals("AttributeInvoke\\MyController::__invoke", route.getController());

        RouteInterface route2 = getFirstValue("attributeinvoke_noname__invoke");
        assertEquals("/foo-attributes/no-name", route2.getPath());
        assertEquals("AttributeInvoke\\NoNameController::__invoke", route2.getController());
    }

    public void testThatPhpFluentRoutesAreIndexed() {
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
            "        final public function defaults(array $defaults): static {}\n" +
            "        final public function methods(array $methods): static {}\n" +
            "    }\n" +
            "\n" +
            "    trait AddTrait\n" +
            "    {\n" +
            "        public function add(string $name, string|array $path): RouteConfigurator {}\n" +
            "        public function prefix(string $prefix): static {}\n" +
            "        public function namePrefix(string $namePrefix): static {}\n" +
            "    }\n" +
            "}\n");

        myFixture.addFileToProject("config/routes.php", "<?php\n" +
            "use Symfony\\Component\\Routing\\Loader\\Configurator\\RoutingConfigurator;\n" +
            "\n" +
            "return static function (RoutingConfigurator $routes): void {\n" +
            "    $route = $routes->add('_profiler_home', '/');\n" +
            "    $route->controller('web_profiler.controller.profiler::homeAction');\n" +
            "\n" +
            "    $routes->add('app_class_route', '/class')\n" +
            "        ->controller(\\App\\Controller\\MyController::class);\n" +
            "\n" +
            "    $routes->add('app_array_route', '/array')\n" +
            "        ->controller([\\App\\Controller\\MyController::class, 'detail']);\n" +
            "\n" +
            "    $group = $routes->namePrefix('admin_')->prefix('/admin');\n" +
            "    $group->add('app_default_route', '/defaults/{id}')\n" +
            "        ->defaults(['_controller' => [\\App\\Controller\\MyController::class, 'detail']])\n" +
            "        ->methods(['GET', 'HEAD']);\n" +
            "\n" +
            "    $group->namePrefix('api_')->prefix('/api')->add('dashboard', '/dashboard');\n" +
            "};");

        myFixture.addFileToProject("src/MyController.php", "<?php\n" +
            "namespace App\\Controller;\n" +
            "class MyController\n" +
            "{\n" +
            "    public function __invoke() {}\n" +
            "    public function detail() {}\n" +
            "}\n");

        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n");

        assertIndexContainsKeyWithValue(RoutesStubIndex.KEY, "_profiler_home",
            value -> "_profiler_home".equals(value.getName()) &&
                "/".equals(value.getPath()) &&
                "web_profiler.controller.profiler::homeAction".equals(value.getController())
        );

        assertIndexContainsKeyWithValue(RoutesStubIndex.KEY, "app_class_route",
            value -> "App\\Controller\\MyController".equals(value.getController())
        );

        assertIndexContainsKeyWithValue(RoutesStubIndex.KEY, "app_array_route",
            value -> "App\\Controller\\MyController::detail".equals(value.getController())
        );

        assertIndexContainsKeyWithValue(RoutesStubIndex.KEY, "admin_app_default_route",
            value -> "admin_app_default_route".equals(value.getName()) &&
                "/admin/defaults/{id}".equals(value.getPath()) &&
                "App\\Controller\\MyController::detail".equals(value.getController()) &&
                value.getMethods().contains("get") &&
                value.getMethods().contains("head")
        );

        assertIndexContainsKeyWithValue(RoutesStubIndex.KEY, "admin_api_dashboard",
            value -> "admin_api_dashboard".equals(value.getName()) &&
                "/admin/api/dashboard".equals(value.getPath())
        );
    }

    @NotNull
    private RouteInterface getFirstValue(@NotNull String key) {
        return FileBasedIndex.getInstance().getValues(RoutesStubIndex.KEY, key, GlobalSearchScope.allScope(getProject())).get(0);
    }
}
