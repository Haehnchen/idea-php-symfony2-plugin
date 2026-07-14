package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata.driver;

import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrineMappingDriverArguments;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrineYamlMappingDriver;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see DoctrineYamlMappingDriver
 */
public class DoctrineYamlMappingDriverTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("yaml-address.orm.yml");
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("doctrine-yaml-mapping.yml"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/metadata/driver/fixtures";
    }

    /**
     * @see DoctrineYamlMappingDriver#getMetadata(DoctrineMappingDriverArguments)
     */
    public void testTable() {
        assertEquals("yaml_entity", createMetadata().getTable());
    }

    /**
     * @see DoctrineYamlMappingDriver#getMetadata(DoctrineMappingDriverArguments)
     */
    public void testFields() {
        DoctrineMetadataModel metadata = createMetadata();

        assertEquals("integer", metadata.getField("id").getTypeName());

        DoctrineModelField nameField = metadata.getField("name");
        assertNotNull(nameField);
        assertEquals("string", nameField.getTypeName());
        assertEquals("user_name", nameField.getColumn());
    }

    /**
     * @see DoctrineYamlMappingDriver#getMetadata(DoctrineMappingDriverArguments)
     */
    public void testEnumType() {
        DoctrineModelField statusField = createMetadata().getField("status");
        assertNotNull(statusField);
        assertEquals("string", statusField.getTypeName());
        assertEquals("\\ORM\\Foobar\\Status", statusField.getEnumType());
    }

    public void testEmbeddedFields() {
        DoctrineMetadataModel metadata = createMetadata();

        DoctrineModelField city = metadata.getField("addressDetails.city");
        assertNotNull(city);
        assertEquals("string", city.getTypeName());
        assertEquals("details_city_name", city.getColumn());

        DoctrineModelField status = metadata.getField("addressDetails.status");
        assertNotNull(status);
        assertEquals("\\ORM\\Foobar\\Status", status.getEnumType());

        assertEquals("city_name", metadata.getField("location.city").getColumn());
    }

    public void testEmbeddableFields() {
        DoctrineMetadataModel metadata = DoctrineMetadataUtil.getModelFields(getProject(), "Doctrine\\Tests\\ORM\\YamlAddress");

        assertNotNull(metadata);
        assertNotNull(metadata.getField("city"));
        assertEquals("\\ORM\\Foobar\\Status", metadata.getField("status").getEnumType());
    }

    /**
     * @see DoctrineYamlMappingDriver#getMetadata(DoctrineMappingDriverArguments)
     */
    public void testRelations() {
        DoctrineMetadataModel metadata = createMetadata();

        assertEquals("oneToOne", metadata.getField("address").getRelationType());
        assertEquals("Address", metadata.getField("address").getRelation());

        assertEquals("oneToMany", metadata.getField("phonenumbers").getRelationType());
        assertEquals("Phonenumber", metadata.getField("phonenumbers").getRelation());

        assertEquals("manyToMany", metadata.getField("groups").getRelationType());
        assertEquals("Group", metadata.getField("groups").getRelation());

        assertEquals("manyToOne", metadata.getField("author").getRelationType());
        assertEquals("Author", metadata.getField("author").getRelation());
    }

    private DoctrineMetadataModel createMetadata() {
        return createMetadata("Doctrine\\Tests\\ORM\\YamlEntity");
    }

    private DoctrineMetadataModel createMetadata(String className) {
        return new DoctrineYamlMappingDriver().getMetadata(
            new DoctrineMappingDriverArguments(getProject(), myFixture.getFile(), className)
        );
    }
}
