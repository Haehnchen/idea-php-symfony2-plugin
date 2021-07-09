package fr.adrienbrault.idea.symfony2plugin.tests.templating.webpack;

import fr.adrienbrault.idea.symfony2plugin.templating.webpack.SymfonyWebpackUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyWebpackUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/webpack/fixtures";
    }

    public void testVisitEntries() {
        myFixture.copyFileToProject("webpack.config.js");
        myFixture.copyFileToProject("entrypoints.json");

        Set<String> entries = new HashSet<>();

        SymfonyWebpackUtil.visitAllEntryFileTypes(myFixture.getProject(), pair -> entries.add(pair.second));
        assertContainsElements(entries, "foo", "foobar", "entry_foobar_2", "addStyleEntryFoobar");
    }
}
