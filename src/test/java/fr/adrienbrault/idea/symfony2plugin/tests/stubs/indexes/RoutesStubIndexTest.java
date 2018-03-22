package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfony2plugin.routing.dict.RouteInterface;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.io.File;

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
            "foo_xml_pattern", "foo_xml_path", "foo_xml_id_only"
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

    @NotNull
    private RouteInterface getFirstValue(@NotNull String key) {
        return FileBasedIndex.getInstance().getValues(RoutesStubIndex.KEY, key, GlobalSearchScope.allScope(getProject())).get(0);
    }
}
