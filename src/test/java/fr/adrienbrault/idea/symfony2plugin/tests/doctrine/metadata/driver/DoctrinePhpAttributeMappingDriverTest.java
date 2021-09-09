package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata.driver;

import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
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
    }

    private DoctrineMetadataModel createOrmMetadata() {
        return new DoctrinePhpAttributeMappingDriver().getMetadata(
            new DoctrineMappingDriverArguments(getProject(), PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php $foo = null;"), "\\ORM\\Attributes\\AttributeEntity")
        );
    }
}
