package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata.driver;

import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrineMappingDriverArguments;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrinePhpMappingDriver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrinePhpMappingDriver
 */
public class DoctrinePhpMappingDriverTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/metadata/driver/fixtures";
    }

    /**
     * @see DoctrinePhpMappingDriver#getMetadata(fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrineMappingDriverArguments)
     */
    public void testPhpAnnotationsMetadata() {

        DoctrineMetadataModel metadata = createOrmMetadata();

        assertEquals("string", metadata.getField("email").getTypeName());

        assertEquals("OneToMany", metadata.getField("phonenumbers").getRelationType());
        assertEquals("Phonenumber", metadata.getField("phonenumbers").getRelation());

        assertEquals("OneToOne", metadata.getField("address").getRelationType());
        assertEquals("Address", metadata.getField("address").getRelation());

        assertEquals("ManyToOne", metadata.getField("apple").getRelationType());
        assertEquals("Apple", metadata.getField("apple").getRelation());

        assertEquals("ManyToMany", metadata.getField("egg").getRelationType());
        assertEquals("Egg", metadata.getField("egg").getRelation());
    }

    /**
     * @see DoctrinePhpMappingDriver#getMetadata(fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrineMappingDriverArguments)
     */
    public void testPhpFlowAnnotationsMetadata() {
        PsiFile psiFile = PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php $foo = null;");

        DoctrineMetadataModel metadata = new DoctrinePhpMappingDriver().getMetadata(
            new DoctrineMappingDriverArguments(getProject(), psiFile, "\\Doctrine\\Flow\\Orm\\Annotation")
        );

        assertEquals("string", metadata.getField("email").getTypeName());

        assertEquals("ManyToMany", metadata.getField("car").getRelationType());
        assertEquals("\\DateTime", metadata.getField("car").getRelation());
    }

    /**
     * @see DoctrinePhpMappingDriver#getMetadata(fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.DoctrineMappingDriverArguments)
     */
    public void testPhpTableAnnotationsMetadata() {
        assertEquals("FOO", createOrmMetadata().getTable());
    }

    private DoctrineMetadataModel createOrmMetadata() {
        return new DoctrinePhpMappingDriver().getMetadata(
            new DoctrineMappingDriverArguments(getProject(), PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php $foo = null;"), "\\Doctrine\\Orm\\Annotation")
        );
    }
}
