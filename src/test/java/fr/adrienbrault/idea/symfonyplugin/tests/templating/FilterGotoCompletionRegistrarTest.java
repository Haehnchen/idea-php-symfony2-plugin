package fr.adrienbrault.idea.symfonyplugin.tests.templating;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfonyplugin.templating.FilterGotoCompletionRegistrar;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see FilterGotoCompletionRegistrar
 */
public class FilterGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("FilterGotoCompletionRegistrarTest.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/templating/fixtures";
    }

    public void testCompletionForTwigFilterTagIdentifier() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% filter <caret> %}", "foobar");
    }

    public void testNavigationForTwigFilterTagIdentifier() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% filter foo<caret>bar %}", "foobar");
    }
}
