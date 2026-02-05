package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata.driver;

import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrineMappingDriverArguments;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrineXmlMappingDriver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see DoctrineXmlMappingDriver
 */
public class DoctrineXmlMappingDriverTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("doctrine-xml-mapping.xml"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/metadata/driver/fixtures";
    }

    /**
     * @see DoctrineXmlMappingDriver#getMetadata(DoctrineMappingDriverArguments)
     */
    public void testTable() {
        assertEquals("xml_entity", createMetadata().getTable());
    }

    /**
     * @see DoctrineXmlMappingDriver#getMetadata(DoctrineMappingDriverArguments)
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
     * @see DoctrineXmlMappingDriver#getMetadata(DoctrineMappingDriverArguments)
     */
    public void testEnumType() {
        DoctrineModelField statusField = createMetadata().getField("status");
        assertNotNull(statusField);
        assertEquals("string", statusField.getTypeName());
        assertEquals("\\ORM\\Foobar\\Status", statusField.getEnumType());
    }

    /**
     * @see DoctrineXmlMappingDriver#getMetadata(DoctrineMappingDriverArguments)
     */
    public void testRelations() {
        DoctrineMetadataModel metadata = createMetadata();

        assertEquals("OneToOne", metadata.getField("address").getRelationType());
        assertEquals("Address", metadata.getField("address").getRelation());

        assertEquals("OneToMany", metadata.getField("phonenumbers").getRelationType());
        assertEquals("Phonenumber", metadata.getField("phonenumbers").getRelation());

        assertEquals("ManyToMany", metadata.getField("groups").getRelationType());
        assertEquals("Group", metadata.getField("groups").getRelation());

        assertEquals("ManyToOne", metadata.getField("author").getRelationType());
        assertEquals("Author", metadata.getField("author").getRelation());
    }

    private DoctrineMetadataModel createMetadata() {
        return new DoctrineXmlMappingDriver().getMetadata(
            new DoctrineMappingDriverArguments(getProject(), myFixture.getFile(), "Doctrine\\Tests\\ORM\\XmlEntity")
        );
    }
}
