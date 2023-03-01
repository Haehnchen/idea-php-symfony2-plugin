package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineManagerEnum;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.yaml.YAMLFileType;

import java.util.*;

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
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/metadata/util/fixtures";
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
        assertEquals("Foo\\Repository", DoctrineMetadataUtil.getClassRepository(getProject(), "Foo\\Car").getPresentableFQN());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil#getTables
     */
    public void testGetTables() {

        Map<String, PsiElement> items = new HashMap<>();

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

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil#getModels
     */
    public void testGetModels() {

        Set<String> classes = new HashSet<>();
        for (PhpClass phpClass : DoctrineMetadataUtil.getModels(getProject())) {
            classes.add(phpClass.getPresentableFQN());
        }

        assertContainsElements(classes, "Doctrine\\Tests\\ORM\\Mapping\\YamlUser");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil#getObjectRepositoryLookupElements
     */
    public void testGetObjectRepositoryLookupElements() {
        assertNotNull(ContainerUtil.find(
            DoctrineMetadataUtil.getObjectRepositoryLookupElements(getProject()),
            lookupElement -> lookupElement.getLookupString().equals("Foo\\Bar\\Repository\\FooBarRepository")
        ));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil#findManagerByScope
     */
    public void testFindManagerByScope() {
        for (Pair<DoctrineManagerEnum, String> pair : Arrays.asList(
            Pair.create(DoctrineManagerEnum.ORM, "orm"),
            Pair.create(DoctrineManagerEnum.COUCHDB, "couchdb"),
            Pair.create(DoctrineManagerEnum.MONGODB, "mongodb"),
            Pair.create(DoctrineManagerEnum.ODM, "odm"),
            Pair.create(DoctrineManagerEnum.DOCUMENT, "document")
        )) {
            assertEquals(pair.getFirst(), DoctrineMetadataUtil.findManagerByScope(myFixture.configureByText("foo." + pair.getSecond() + ".yml", "")));
            assertEquals(pair.getFirst(), DoctrineMetadataUtil.findManagerByScope(myFixture.configureByText("foo." + pair.getSecond() + ".yaml", "")));
            assertEquals(pair.getFirst(), DoctrineMetadataUtil.findManagerByScope(myFixture.configureByText("foo." + pair.getSecond() + ".xml", "")));
            assertEquals(pair.getFirst(), DoctrineMetadataUtil.findManagerByScope(myFixture.configureByText("foo." + pair.getSecond() + ".XML", "")));
        }
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil#findMetadataForRepositoryClass
     */
    public void testFindMetadataForRepositoryClass() {
        Condition<VirtualFile> condition = virtualFile -> virtualFile.getName().equals("doctrine.odm.xml");

        assertNotNull(ContainerUtil.find(DoctrineMetadataUtil.findMetadataForRepositoryClass(getProject(), "Foo\\Bar\\Repository\\FooBarRepository"), condition));
        assertNotNull(ContainerUtil.find(DoctrineMetadataUtil.findMetadataForRepositoryClass(getProject(), "Entity\\BarRepository"), condition));
        assertNotNull(ContainerUtil.find(DoctrineMetadataUtil.findMetadataForRepositoryClass(PhpElementsUtil.getClassInterface(getProject(), "Entity\\BarRepository")), condition));
        assertNull(ContainerUtil.find(DoctrineMetadataUtil.findMetadataForRepositoryClass(getProject(), "Entity\\BarEmpty"), condition));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil#findModelNameInScope
     */
    public void testFindModelNameInScope() {
        for (String s : new String[]{
            "<doctrine-foo-mapping><document name=\"Foo\\Foo\"><id attr=\"b<caret>a\"></document></doctrine-foo-mapping>",
            "<doctrine-foo-mapping><entity name=\"Foo\\Foo\"><id attr=\"b<caret>a\"></entity></doctrine-foo-mapping>",
            "<doctrine-foo-mapping><embedded-document name=\"Foo\\Foo\"><id attr=\"b<caret>a\"></embedded-document></doctrine-foo-mapping>",
            "<doctrine-foo-mapping><embedded name=\"Foo\\Foo\"><id attr=\"b<caret>a\"></embedded></doctrine-foo-mapping>",
        }) {
            myFixture.configureByText(XmlFileType.INSTANCE, s);
            PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
            assertEquals("Foo\\Foo", DoctrineMetadataUtil.findModelNameInScope(psiElement));
        }
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil#findModelNameInScope
     */
    public void testYamlFindModelNameInScope() {
        for (String s : new String[]{
            "Foo\\Foo: \n   type: enti<caret>tiy",
            "\n   \n   \nFoo\\Foo: \n   type: enti<caret>tiy",
        }) {
            myFixture.configureByText(YAMLFileType.YML, s);
            PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
            assertEquals(String.format("Fixture %s", s), "Foo\\Foo", DoctrineMetadataUtil.findModelNameInScope(psiElement));
        }
    }
}
