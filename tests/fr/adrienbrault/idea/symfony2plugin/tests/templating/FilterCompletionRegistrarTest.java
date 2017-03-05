package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.FilterCompletionRegistrar
 */
public class FilterCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("FilterCompletionRegistrarTest.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testCompletionForTwigFilterTagIdentifier() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% filter <caret> %}", "foobar");
    }

    public void testNavigationForTwigFilterTagIdentifier() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% filter foo<caret>bar %}", "foobar");
    }
}
