package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.SerializerClassUsageStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.SerializerClassUsageStubIndex
 */
public class SerializerClassUsageStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("SerializerStubIndex.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/indexes/fixtures";
    }

    public void testSerializerIdIndex() {
        assertIndexContains(SerializerClassUsageStubIndex.KEY, "\\app\\foobar", "\\app\\foobar2", "\\app\\foobar3", "\\app\\foobar4");
    }
}
