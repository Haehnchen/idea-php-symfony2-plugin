package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.DoctrineMetadataFileStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.DoctrineMetadataFileStubIndex
 */
public class DoctrineMetadataFileStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureByText("doctrine.mongodb.xml", "" +
            "<doctrine-mongo-mapping >\n" +
            "    <document name=\"Documents\\Xml\\MongoUser\" db=\"documents\" collection=\"users\" repository-class=\"Documents\\Xml\\MongoUserRepository\"/>\n" +
            "    <document name=\"Documents\\User1\" db=\"documents\" collection=\"users\"/>\n" +
            "</doctrine-mongo-mapping>"
        );

        myFixture.configureByText("doctrine.orm.xml",
            "<doctrine-mapping>\n" +
            "    <entity name=\"Documents\\Xml\\OrmUser\" table=\"cms_users\" repository-class=\"Documents\\Xml\\OrmUserRepository\"/>\n" +
            "</doctrine-mapping>"
        );

        myFixture.configureByText("doctrine.yml", "" +
            "Documents\\Yml\\OdmUser:\n" +
            "  db: documents\n" +
            "\n" +
            "Documents\\Yml\\OrmUser:\n" +
            "  repositoryClass: Documents\\Yml\\OrmUserRepository"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineUtil#getClassRepositoryPair
     */
    public void testXmlMetadata() {
        assertIndexContains(DoctrineMetadataFileStubIndex.KEY, "Documents\\Xml\\MongoUser");
        assertIndexContains(DoctrineMetadataFileStubIndex.KEY, "Documents\\Xml\\OrmUser");

        assertIndexContainsKeyWithValue(DoctrineMetadataFileStubIndex.KEY, "Documents\\Xml\\MongoUser", "Documents\\Xml\\MongoUserRepository");
        assertIndexContainsKeyWithValue(DoctrineMetadataFileStubIndex.KEY, "Documents\\Xml\\OrmUser", "Documents\\Xml\\OrmUserRepository");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineUtil#getClassRepositoryPair
     */
    public void testXmlMetadataInvalid() {
        myFixture.configureByText("doctrine.mongodb.xml", "" +
            "<foo-mongo-mapping >\n" +
            "    <document name=\"Documents\\Invalid\" db=\"documents\" collection=\"users\"/>\n" +
            "</foo-mongo-mapping>"
        );

        assertIndexNotContains(DoctrineMetadataFileStubIndex.KEY, "Documents\\Invalid");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineUtil#getClassRepositoryPair
     */
    public void testYamlMetadata() {
        myFixture.configureByFile("doctrine.yml");

        assertIndexContains(DoctrineMetadataFileStubIndex.KEY, "Documents\\Yml\\OdmUser");
        assertIndexContains(DoctrineMetadataFileStubIndex.KEY, "Documents\\Yml\\OrmUser");

        assertIndexContainsKeyWithValue(DoctrineMetadataFileStubIndex.KEY, "Documents\\Yml\\OrmUser", "Documents\\Yml\\OrmUserRepository");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineUtil#getClassRepositoryPair
     */
    public void testYamlMetadataValidByFilename() {
        myFixture.configureByText("doctrine.orm.yml", "" +
            "Filename:\n" +
            "  foo: foo"
        );

        assertIndexContains(DoctrineMetadataFileStubIndex.KEY, "Filename");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineUtil#getClassRepositoryPair
     */
    public void testYamlMetadataValidByStructure() {
        myFixture.configureByText("foo.yml", "" +
            "FooFields:\n" +
            "  fields: ~"
        );
        assertIndexContains(DoctrineMetadataFileStubIndex.KEY, "FooFields");

        myFixture.configureByText("foo.yml", "" +
            "FooId:\n" +
            "  id: ~"
        );
        assertIndexContains(DoctrineMetadataFileStubIndex.KEY, "FooId");

        myFixture.configureByText("foo.yml", "" +
            "FooCollection:\n" +
            "  collection: ~"
        );
        assertIndexContains(DoctrineMetadataFileStubIndex.KEY, "FooCollection");

        myFixture.configureByText("foo.yml", "" +
            "FooDb:\n" +
            "  db: ~"
        );
        assertIndexContains(DoctrineMetadataFileStubIndex.KEY, "FooDb");

        myFixture.configureByText("foo.yml", "" +
            "FooIndexes:\n" +
            "  indexes: ~"
        );
        assertIndexContains(DoctrineMetadataFileStubIndex.KEY, "FooIndexes");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineUtil#getClassRepositoryPair
     */
    public void testYamlMetadataInvalidByFilename() {
        myFixture.configureByText("doctrine_foo.yml", "" +
            "Documents\\Yml\\OrmUserInvalid:\n" +
            "  foo: ~"
        );

        assertIndexNotContains(DoctrineMetadataFileStubIndex.KEY, "Documents\\Yml\\OrmUserInvalid");
    }
}
