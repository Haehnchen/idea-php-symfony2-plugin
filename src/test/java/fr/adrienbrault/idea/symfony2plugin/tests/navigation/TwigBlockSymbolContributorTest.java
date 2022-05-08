package fr.adrienbrault.idea.symfony2plugin.tests.navigation;

import fr.adrienbrault.idea.symfony2plugin.navigation.TwigBlockSymbolContributor;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Arrays;

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

        assertContainsElements(
            Arrays.asList(contributor.getNames(getProject(), false)),
            "my_support_block_name",
            "my_support_block_name_2"
        );
    }

    public void testThatBlockNavigationIsProvidedForSearch() {
        TwigBlockSymbolContributor contributor = new TwigBlockSymbolContributor();

        assertTrue(Arrays.stream(contributor.getItemsByName("my_support_block_name", "?", getProject(), false))
            .anyMatch(navigationItem -> "my_support_block_name".equals(navigationItem.getName())));

        assertTrue(Arrays.stream(contributor.getItemsByName("my_support_block_name_2", "?", getProject(), false))
            .anyMatch(navigationItem -> "my_support_block_name_2".equals(navigationItem.getName())));

        assertSize(0, contributor.getItemsByName("UNKNOWN_BLOCK", "?", getProject(), false));
    }
}
