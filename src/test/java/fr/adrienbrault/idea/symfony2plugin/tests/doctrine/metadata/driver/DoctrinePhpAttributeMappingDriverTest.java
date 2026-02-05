package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata.driver;

import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrineMappingDriverArguments;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrinePhpAttributeMappingDriver;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrinePhpMappingDriver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrinePhpAttributeMappingDriver
 */
public class DoctrinePhpAttributeMappingDriverTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("attributes.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/metadata/driver/fixtures";
    }

    /**
     * @see DoctrinePhpMappingDriver#getMetadata(fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrineMappingDriverArguments)
     */
    public void testPhpAttributesMetadata() {
        DoctrineMetadataModel metadata = createOrmMetadata();

        assertEquals("table_name", metadata.getTable());

        assertEquals("string", metadata.getField("email").getTypeName());
        assertEquals("string", metadata.getField("emailTrait").getTypeName());

        assertEquals("\\ORM\\Foobar\\Egg", metadata.getField("apple").getRelation());
        assertEquals("ManyToOne", metadata.getField("apple").getRelationType());

        assertEquals("\\ORM\\Foobar\\Egg", metadata.getField("egg").getRelation());
        assertEquals("ManyToMany", metadata.getField("egg").getRelationType());

        assertEquals("\\ORM\\Foobar\\Egg", metadata.getField("address").getRelation());
        assertEquals("OneToOne", metadata.getField("address").getRelationType());

        assertEquals("\\ORM\\Foobar\\Egg", metadata.getField("phonenumbers").getRelation());
        assertEquals("OneToMany", metadata.getField("phonenumbers").getRelationType());

        assertEquals("\\Doctrine\\Orm\\MyTrait\\Egg", metadata.getField("appleTrait").getRelation());
        assertEquals("ManyToOne", metadata.getField("appleTrait").getRelationType());

        assertEquals("\\ORM\\Foobar\\Egg", metadata.getField("eggClassString").getRelation());
        assertEquals("ManyToMany", metadata.getField("eggClassString").getRelationType());

        assertEquals("\\ORM\\Foobar\\Egg", metadata.getField("eggClassStringBackslashless").getRelation());
        assertEquals("ManyToMany", metadata.getField("eggClassStringBackslashless").getRelationType());

        assertEquals("\\ORM\\Foobar\\Egg", metadata.getField("eggTargetEntity").getRelation());
        assertEquals("ManyToMany", metadata.getField("eggTargetEntity").getRelationType());
    }

    /**
     * @see DoctrinePhpAttributeMappingDriver#getMetadata(DoctrineMappingDriverArguments)
     */
    public void testColumnOptions() {
        DoctrineMetadataModel metadata = createOrmMetadata();

        // Test email field: #[ORM\Column(type: "string", length: 32, unique: true, nullable: false)]
        DoctrineModelField emailField = metadata.getField("email");
        assertNotNull(emailField);
        assertEquals("string", emailField.getTypeName());
        assertEquals(Integer.valueOf(32), emailField.getLength());
        assertEquals(Boolean.TRUE, emailField.getUnique());
        assertEquals(Boolean.FALSE, emailField.getNullable());
    }

    /**
     * @see DoctrinePhpAttributeMappingDriver#getMetadata(DoctrineMappingDriverArguments)
     */
    public void testColumnDecimalOptions() {
        DoctrineMetadataModel metadata = createOrmMetadata();

        // Test price field: #[ORM\Column(type: "decimal", precision: 10, scale: 2, nullable: true)]
        DoctrineModelField priceField = metadata.getField("price");
        assertNotNull(priceField);
        assertEquals("decimal", priceField.getTypeName());
        assertEquals(Boolean.TRUE, priceField.getNullable());
        assertEquals("string", priceField.getPropertyType()); // primitive type stays as-is
    }

    /**
     * @see DoctrinePhpAttributeMappingDriver#getMetadata(DoctrineMappingDriverArguments)
     */
    public void testEnumType() {
        DoctrineMetadataModel metadata = createOrmMetadata();

        // Test status field: #[ORM\Column(type: "string", enumType: Status::class)]
        DoctrineModelField statusField = metadata.getField("status");
        assertNotNull(statusField);
        assertEquals("string", statusField.getTypeName());
        assertEquals("\\ORM\\Foobar\\Status", statusField.getEnumType());
        assertEquals("\\ORM\\Foobar\\Status", statusField.getPropertyType()); // class type resolved to FQN
    }

    private DoctrineMetadataModel createOrmMetadata() {
        return new DoctrinePhpAttributeMappingDriver().getMetadata(
            new DoctrineMappingDriverArguments(getProject(), PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php $foo = null;"), "\\ORM\\Attributes\\AttributeEntity")
        );
    }
}
