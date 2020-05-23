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
        assertEquals("string", metadata.getField("emailTrait").getTypeName());

        assertEquals("OneToMany", metadata.getField("phonenumbers").getRelationType());
        assertEquals("\\Doctrine\\Orm\\Phonenumber", metadata.getField("phonenumbers").getRelation());

        assertEquals("OneToOne", metadata.getField("address").getRelationType());
        assertEquals("\\Doctrine\\Orm\\Address", metadata.getField("address").getRelation());

        assertEquals("ManyToOne", metadata.getField("apple").getRelationType());
        assertEquals("\\Doctrine\\Orm\\Apple", metadata.getField("apple").getRelation());

        assertEquals("ManyToMany", metadata.getField("egg").getRelationType());
        assertEquals("\\Doctrine\\Orm\\Egg", metadata.getField("egg").getRelation());

        assertEquals("ManyToMany", metadata.getField("egg").getRelationType());
        assertEquals("\\Doctrine\\Orm\\Egg", metadata.getField("egg").getRelation());

        assertEquals("ManyToMany", metadata.getField("eggClass").getRelationType());
        assertEquals("\\Doctrine\\Egg", metadata.getField("eggClass").getRelation());

        assertEquals("ManyToMany", metadata.getField("eggSelfAlias").getRelationType());
        assertEquals("\\Doctrine\\Egg", metadata.getField("eggSelfAlias").getRelation());

        assertEquals("ManyToMany", metadata.getField("eggSelfAliasFooBar").getRelationType());
        assertEquals("\\Doctrine\\Egg\\Foo\\Bar", metadata.getField("eggSelfAliasFooBar").getRelation());

        assertEquals("ManyToOne", metadata.getField("appleTrait").getRelationType());
        assertEquals("\\Doctrine\\Apple", metadata.getField("appleTrait").getRelation());

        assertEquals("ManyToOne", metadata.getField("appleExtends").getRelationType());
        assertEquals("\\Doctrine\\FooBar", metadata.getField("appleExtends").getRelation());
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
