package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil
 */
public class DoctrineMetadataUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("doctrine.odm.xml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("doctrine.orm.xml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("doctrine.orm.yml"));

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil#findMetadataFiles
     */
    public void testFindMetadataFiles() {
        assertSize(1, DoctrineMetadataUtil.findMetadataFiles(getProject(), "Foo\\Bar"));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil#getModelFields
     */
    public void testGetModelFields() {
        DoctrineMetadataModel modelFields = DoctrineMetadataUtil.getModelFields(getProject(), "Foo\\Bar");

        assertEquals("string", modelFields.getField("foo1").getTypeName());
        assertEquals("mixed", modelFields.getField("foo2").getTypeName());
        assertEquals("string", modelFields.getField("foo3").getTypeName());

        assertEquals("reference-one", modelFields.getField("apple1").getRelationType());
        assertEquals("Foo\\Bar\\Apple", modelFields.getField("apple1").getRelation());

        assertEquals("embed-one", modelFields.getField("egg1").getRelationType());
        assertEquals("Foo\\Bar\\Egg", modelFields.getField("egg1").getRelation());

        assertEquals("reference-many", modelFields.getField("apple2").getRelationType());
        assertEquals("Foo\\Bar\\Apple", modelFields.getField("apple2").getRelation());

        assertEquals("embed-many", modelFields.getField("egg2").getRelationType());
        assertEquals("Foo\\Bar\\Egg", modelFields.getField("egg2").getRelation());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil#getModelFields
     */
    public void testGetOrmXmlModelFields() {
        DoctrineMetadataModel modelFields = DoctrineMetadataUtil.getModelFields(getProject(), "Doctrine\\Tests\\ORM\\Mapping\\XmlUser");

        assertEquals("integer", modelFields.getField("id").getTypeName());

        assertEquals("string", modelFields.getField("name").getTypeName());
        assertEquals("string", modelFields.getField("email").getTypeName());

        assertEquals("Address", modelFields.getField("address").getRelation());
        assertEquals("Phonenumber", modelFields.getField("phonenumbers").getRelation());
        assertEquals("Group", modelFields.getField("groups").getRelation());
        assertEquals("Author", modelFields.getField("author").getRelation());

        assertEquals("OneToOne", modelFields.getField("address").getRelationType());
        assertEquals("OneToMany", modelFields.getField("phonenumbers").getRelationType());
        assertEquals("ManyToMany", modelFields.getField("groups").getRelationType());
        assertEquals("ManyToOne", modelFields.getField("author").getRelationType());

        assertEquals("cms_users", modelFields.getTable());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil#getModelFields
     */
    public void testGetOrmYmlModelFields() {
        DoctrineMetadataModel modelFields = DoctrineMetadataUtil.getModelFields(getProject(), "Doctrine\\Tests\\ORM\\Mapping\\YamlUser");

        assertEquals("integer", modelFields.getField("id").getTypeName());

        assertEquals("string", modelFields.getField("name").getTypeName());
        assertEquals("string", modelFields.getField("email").getTypeName());

        assertEquals("Address", modelFields.getField("address").getRelation());
        assertEquals("Phonenumber", modelFields.getField("phonenumbers").getRelation());
        assertEquals("Group", modelFields.getField("groups").getRelation());
        assertEquals("Author", modelFields.getField("author").getRelation());

        assertEquals("oneToOne", modelFields.getField("address").getRelationType());
        assertEquals("oneToMany", modelFields.getField("phonenumbers").getRelationType());
        assertEquals("manyToMany", modelFields.getField("groups").getRelationType());
        assertEquals("manyToOne", modelFields.getField("author").getRelationType());

        assertEquals("foo_table", modelFields.getTable());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil#getClassRepository
     */
    public void testGetClassRepository() {
        assertEquals("Doctrine\\Tests\\ORM\\Mapping\\YamlUserRepository", DoctrineMetadataUtil.getClassRepository(getProject(), "Doctrine\\Tests\\ORM\\Mapping\\YamlUser").getPresentableFQN());
        assertEquals("Foo\\Bar\\Repository\\FooBarRepository", DoctrineMetadataUtil.getClassRepository(getProject(), "Foo\\Bar").getPresentableFQN());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil#getTables
     */
    public void testGetTables() {

        Map<String, PsiElement> items = new HashMap<String, PsiElement>();

        Collection<Pair<String, PsiElement>> tables = DoctrineMetadataUtil.getTables(getProject());
        for (Pair<String, PsiElement> pair : tables) {
            items.put(pair.getFirst(), pair.getSecond());
        }

        assertContainsElements(items.keySet(), "cms_users", "foo_table");

        assertNotNull(items.get("cms_users"));
        assertNotNull(items.get("foo_table"));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil#getMetadataByTable
     */
    public void testGetMetadataByTable() {
        assertNotNull(DoctrineMetadataUtil.getMetadataByTable(getProject(), "cms_users").getField("id"));
        assertNotNull(DoctrineMetadataUtil.getMetadataByTable(getProject(), "cms_users").getField("name"));
    }
}
