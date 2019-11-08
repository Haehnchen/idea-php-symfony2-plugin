package fr.adrienbrault.idea.symfonyplugin.tests.stubs.indexes;

import fr.adrienbrault.idea.symfonyplugin.stubs.indexes.ContainerParameterStubIndex;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.stubs.indexes.ContainerParameterStubIndex
 */
public class ContainerParameterStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.xml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.yml"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/stubs/indexes/fixtures";
    }

    public void testThatParameterOfYamlFileIsInIndex() {
        assertIndexContains(ContainerParameterStubIndex.KEY, "foo_yaml_parameter");
        assertIndexContainsKeyWithValue(ContainerParameterStubIndex.KEY, "foo_yaml_parameter", "foo");
        assertIndexContainsKeyWithValue(ContainerParameterStubIndex.KEY, "foo_yaml_parameter_upper", "foo");

        assertIndexContains(ContainerParameterStubIndex.KEY, "foo_yaml_parameter_empty");
        assertIndexContainsKeyWithValue(ContainerParameterStubIndex.KEY, "foo_yaml_parameter_empty", "");
    }

    public void testThatParameterOfYamlInCollectionValueIsBlank() {
        assertIndexContains(ContainerParameterStubIndex.KEY, "foo_yaml_parameter_collection");
        assertIndexContainsKeyWithValue(ContainerParameterStubIndex.KEY, "foo_yaml_parameter_collection", "");

        assertIndexContains(ContainerParameterStubIndex.KEY, "foo_yaml_parameter_collection_sequence");
        assertIndexContainsKeyWithValue(ContainerParameterStubIndex.KEY, "foo_yaml_parameter_collection_sequence", "");
    }

    public void testThatParameterOfXmlFileIsInIndex() {
        assertIndexContains(ContainerParameterStubIndex.KEY, "foo_xml_parameter");
        assertIndexContainsKeyWithValue(ContainerParameterStubIndex.KEY, "foo_xml_parameter", "foo");
    }

    public void testThatParameteOfXmlWithoutValueIsIndexedWithBlankString() {
        assertIndexContains(ContainerParameterStubIndex.KEY, "foo_xml_parameter_empty");
        assertIndexContainsKeyWithValue(ContainerParameterStubIndex.KEY, "foo_xml_parameter_empty", "");
    }

    public void testThatParameterOfXmlWithCollectionValueIsIndexedWithBlankString() {
        assertIndexContains(ContainerParameterStubIndex.KEY, "foo_xml_parameter_empty.collection");
        assertIndexContainsKeyWithValue(ContainerParameterStubIndex.KEY, "foo_xml_parameter_empty.collection", "");
    }

    public void testThatParameterOfXmlConvertedToLowerCase() {
        assertIndexContains(ContainerParameterStubIndex.KEY, "foo_xml_parameter_upper");
    }

    public void testThatParameterOfXmlWithCollectionInvalidValueIsIndexedWithBlankString() {
        assertIndexContains(ContainerParameterStubIndex.KEY, "foo_xml_parameter_empty.collection_invalid");
        assertIndexContainsKeyWithValue(ContainerParameterStubIndex.KEY, "foo_xml_parameter_empty.collection_invalid", "");
    }
}
