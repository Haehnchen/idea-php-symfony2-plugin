package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata.util;

import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see DoctrineMetadataUtil#getTableNames
 */
public class DoctrineMetadataUtilTableNamesTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("doctrine.orm.xml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("doctrine.orm.yml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("attribute_entity.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/metadata/util/fixtures";
    }

    /**
     * @see DoctrineMetadataUtil#getTableNames
     */
    public void testGetTableNamesReturnsCachedResults() {
        Set<String> tableNames = DoctrineMetadataUtil.getTableNames(getProject());
        assertContainsElements(tableNames, "cms_users", "foo_table");
    }

    /**
     * @see DoctrineMetadataUtil#getTableNames
     */
    public void testGetTableNamesWithPhpAttributes() {
        Set<String> tableNames = DoctrineMetadataUtil.getTableNames(getProject());
        assertContainsElements(tableNames, "php_attribute_table");
    }

    /**
     * @see DoctrineMetadataUtil#getTableNames
     */
    public void testGetTableNamesMatchesGetTables() {
        Set<String> cachedNames = DoctrineMetadataUtil.getTableNames(getProject());

        Set<String> originalNames = DoctrineMetadataUtil.getTables(getProject()).stream()
            .map(pair -> pair.getFirst())
            .collect(Collectors.toSet());

        assertEquals("Cached and original should return same table names", originalNames, cachedNames);
    }

    /**
     * @see DoctrineMetadataUtil#getTableNames
     */
    public void testGetTableNamesReturnsSet() {
        Set<String> tableNames = DoctrineMetadataUtil.getTableNames(getProject());
        assertTrue(tableNames instanceof Set);
    }
}
