package fr.adrienbrault.idea.symfony2plugin.tests.navigation;

import com.intellij.navigation.NavigationItem;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FindSymbolParameters;
import fr.adrienbrault.idea.symfony2plugin.navigation.TwigBlockSymbolContributor;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.navigation.TwigBlockSymbolContributor
 */
public class TwigBlockSymbolContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("test.html.twig");
        myFixture.copyFileToProject("test2.html.twig");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/navigation/fixtures";
    }


    public void testThatBlockNamesAreProvidedForSearch() {
        TwigBlockSymbolContributor contributor = new TwigBlockSymbolContributor();

        List<String> names = new ArrayList<>();
        contributor.processNames(names::add, GlobalSearchScope.allScope(getProject()), null);

        assertContainsElements(
            names,
            "my_support_block_name",
            "my_support_block_name_2"
        );
    }

    public void testThatBlockNavigationIsProvidedForSearch() {
        TwigBlockSymbolContributor contributor = new TwigBlockSymbolContributor();

        // Test for "my_support_block_name"
        List<NavigationItem> items1 = new ArrayList<>();
        FindSymbolParameters params1 = FindSymbolParameters.simple(getProject(), false);
        contributor.processElementsWithName("my_support_block_name", items1::add, params1);
        assertTrue(items1.stream().anyMatch(item -> "my_support_block_name".equals(item.getName())));

        // Test for "my_support_block_name_2"
        List<NavigationItem> items2 = new ArrayList<>();
        FindSymbolParameters params2 = FindSymbolParameters.simple(getProject(), false);
        contributor.processElementsWithName("my_support_block_name_2", items2::add, params2);
        assertTrue(items2.stream().anyMatch(item -> "my_support_block_name_2".equals(item.getName())));

        // Test for unknown block
        List<NavigationItem> items3 = new ArrayList<>();
        FindSymbolParameters params3 = FindSymbolParameters.simple(getProject(), false);
        contributor.processElementsWithName("UNKNOWN_BLOCK", items3::add, params3);
        assertSize(0, items3);
    }
}
