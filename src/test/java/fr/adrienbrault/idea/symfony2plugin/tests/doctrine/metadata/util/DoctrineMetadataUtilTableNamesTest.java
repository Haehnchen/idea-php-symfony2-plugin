package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata.util;

import com.intellij.openapi.util.Pair;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see DoctrineMetadataUtil#getTables
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
     * @see DoctrineMetadataUtil#getTables
     */
    public void testGetTableNamesReturnsCachedResults() {
        Set<String> tableNames = DoctrineMetadataUtil.getTables(getProject()).stream()
            .map(p -> p.getFirst())
            .collect(Collectors.toSet());
        assertContainsElements(tableNames, "cms_users", "foo_table");
    }

    /**
     * @see DoctrineMetadataUtil#getTables
     */
    public void testGetTableNamesWithPhpAttributes() {
        Set<String> tableNames = DoctrineMetadataUtil.getTables(getProject()).stream()
            .map(p -> p.getFirst())
            .collect(Collectors.toSet());
        assertContainsElements(tableNames, "php_attribute_table");
    }

    /**
     * @see DoctrineMetadataUtil#getTables
     * Each pair contains (tableName, classFqn); the first element is the table name.
     */
    public void testGetTablesReturnsPairsWithTableNameAndClassFqn() {
        Collection<Pair<String, String>> tables = DoctrineMetadataUtil.getTables(getProject());
        assertFalse("getTables should return at least one entry", tables.isEmpty());
        for (Pair<String, String> pair : tables) {
            assertNotNull("table name must not be null", pair.getFirst());
            assertNotNull("class FQN must not be null", pair.getSecond());
        }
    }
}
