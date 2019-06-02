package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerIdUsagesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ContainerIdUsagesStubIndex
 */
public class ContainerIdUsagesStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("usage.services.xml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("usage.services.yml"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/indexes/fixtures";
    }

    public void testThatXmlArgumentUsageExtracted() {
        assertIndexContains(ContainerIdUsagesStubIndex.KEY, "usage_xml_foobar", "usage_xml_foobar2", "usage_xml_foobar3");

        assertIndexContainsKeyWithValue(ContainerIdUsagesStubIndex.KEY, "usage_xml_foobar", value -> value == 2);
        assertIndexContainsKeyWithValue(ContainerIdUsagesStubIndex.KEY, "usage_xml_foobar2", value -> value == 2);
        assertIndexContainsKeyWithValue(ContainerIdUsagesStubIndex.KEY, "usage_xml_foobar3", value -> value == 1);
    }

    public void testThatXmlArgumentUsageOnSetterArgumentsAreExtracted() {
        assertIndexContains(ContainerIdUsagesStubIndex.KEY, "xml_setter_foobar_1", "xml_setter_foobar_2", "xml_setter_foobar_3");

        assertIndexContainsKeyWithValue(ContainerIdUsagesStubIndex.KEY, "xml_setter_foobar_1", value -> value == 2);
    }

    public void testThatYamlArgumentUsageExtracted() {
        assertIndexContains(ContainerIdUsagesStubIndex.KEY, "usage_yml_foobar", "usage_yml_foobar2", "usage_yml_foobar3");

        assertIndexContainsKeyWithValue(ContainerIdUsagesStubIndex.KEY, "usage_yml_foobar", value -> value == 2);
        assertIndexContainsKeyWithValue(ContainerIdUsagesStubIndex.KEY, "usage_yml_foobar2", value -> value == 2);
        assertIndexContainsKeyWithValue(ContainerIdUsagesStubIndex.KEY, "usage_yml_foobar3", value -> value == 1);
    }

    public void testThatYamlArgumentUsageOnSetterArgumentsAreExtracted() {
        assertIndexContains(ContainerIdUsagesStubIndex.KEY, "yaml_setter_foobar_1", "yaml_setter_foobar_2", "yaml_setter_foobar_3");

        assertIndexContainsKeyWithValue(ContainerIdUsagesStubIndex.KEY, "yaml_setter_foobar_1", value -> value == 2);
    }
}
