package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata.driver;

import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrineMappingDriverArguments;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrineYamlMappingDriver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see DoctrineYamlMappingDriver
 */
public class DoctrineYamlMappingDriverTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
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
        return new DoctrineYamlMappingDriver().getMetadata(
            new DoctrineMappingDriverArguments(getProject(), myFixture.getFile(), "Doctrine\\Tests\\ORM\\YamlEntity")
        );
    }
}
