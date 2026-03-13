package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata.driver;

import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrineMappingDriverArguments;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrineMappingDriverFileCacheDecorator;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrineYamlMappingDriver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Objects;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see DoctrineMappingDriverFileCacheDecorator
 */
public class DoctrineMappingDriverFileCacheDecoratorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("doctrine-yaml-mapping.yml"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/metadata/driver/fixtures";
    }

    public void testDecoratorReturnsCorrectMetadata() {
        DoctrineMetadataModel metadata = createMetadata("Doctrine\\Tests\\ORM\\YamlEntity");

        assertNotNull(metadata);
        assertEquals("yaml_entity", metadata.getTable());
        assertEquals("integer", Objects.requireNonNull(metadata.getField("id")).getTypeName());
        assertEquals("string", Objects.requireNonNull(metadata.getField("name")).getTypeName());
    }

    public void testReturnsNullForUnknownClass() {
        assertNull(createMetadata("Doctrine\\Tests\\ORM\\NonExistent"));
    }

    private DoctrineMetadataModel createMetadata(String className) {
        return new DoctrineMappingDriverFileCacheDecorator(new DoctrineYamlMappingDriver()).getMetadata(
            new DoctrineMappingDriverArguments(getProject(), myFixture.getFile(), className)
        );
    }
}
