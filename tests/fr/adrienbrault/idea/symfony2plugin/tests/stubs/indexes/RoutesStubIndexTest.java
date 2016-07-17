package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex
 */
public class RoutesStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureByText(YAMLFileType.YML, "" +
            "foo_yaml_pattern:\n" +
            "    pattern: /\n" +
            "    methods: [GET, POST]\n" +
            "    defaults: { _controller: foo_controller }" +
            "\n" +
            "foo_yaml_path:\n" +
            "    path: /\n" +
            "    defaults: { _controller: foo_controller }" +
            "\n" +
            "foo_yaml_controller_normalized:\n" +
            "    path: /\n" +
            "    defaults: { _controller: FooBundle:Foo/Foo:index }" +
            "\n" +
            "foo_yaml_path_only:\n" +
            "    path: /\n" +
            "foo_yaml_invalid:\n" +
            "    path_invalid: /\n"
        );

        myFixture.configureByText(XmlFileType.INSTANCE, "" +
            "<routes>\n" +
            "  <route id=\"foo_xml_pattern\" pattern=\"/blog/{slug}\" methods=\"GET|POST\"/>\n" +
            "  <route id=\"foo_xml_path\" path=\"/blog/{slug}\">\n" +
            "    <default key=\"_controller\">Foo</default>\n" +
            "  </route>\n" +
            "  <route id=\"foo_controller_normalized\" path=\"/blog/{slug}\">\n" +
            "    <default key=\"_controller\">FooBundle:Foo/Foo:index</default>\n" +
            "  </route>\n" +
            "  <route id=\"foo_xml_id_only\"/>\n" +
            "</routes>"
        );
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
}
