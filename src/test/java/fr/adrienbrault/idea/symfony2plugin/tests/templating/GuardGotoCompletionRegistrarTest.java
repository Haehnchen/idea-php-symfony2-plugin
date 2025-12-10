package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.GuardGotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see GuardGotoCompletionRegistrar
 */
public class GuardGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("GuardGotoCompletionRegistrarTest.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    /**
     * Test completion for guard type keywords
     */
    public void testCompletionForGuardTypeKeywords() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% guard <caret> %}", "function", "filter", "test");
    }

    /**
     * Test completion for Twig function names after "guard function"
     */
    public void testCompletionForGuardFunction() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% guard function <caret> %}", "test_function");
    }

    /**
     * Test completion for Twig filter names after "guard filter"
     */
    public void testCompletionForGuardFilter() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% guard filter <caret> %}", "test_filter");
    }

    /**
     * Test completion for Twig test names after "guard test"
     */
    public void testCompletionForGuardTest() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% guard test <caret> %}", "test_test");
    }

    /**
     * Test navigation for guard function
     */
    public void testNavigationForGuardFunction() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% guard function test_fun<caret>ction %}", "test_function");
    }

    /**
     * Test navigation for guard filter
     */
    public void testNavigationForGuardFilter() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% guard filter test_fil<caret>ter %}", "test_filter");
    }

    /**
     * Test navigation for guard test
     */
    public void testNavigationForGuardTest() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% guard test test_te<caret>st %}", "test_test");
    }
}
