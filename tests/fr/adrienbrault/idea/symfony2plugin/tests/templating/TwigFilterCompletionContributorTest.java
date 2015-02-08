package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see com.jetbrains.twig.completion.TwigCompletionContributor
 */
public class TwigFilterCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("TwigFilterExtension.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testTwigExtensionFilterCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ 'test'|<caret> }}", "doctrine_minify_query", "doctrine_pretty_query");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ 'test'  |   <caret> }}", "doctrine_minify_query", "doctrine_pretty_query");
        assertCompletionContains(TwigFileType.INSTANCE, "{{     'test'    |       <caret>   }}", "doctrine_minify_query", "doctrine_pretty_query");
    }

    public void testTwigExtensionFilterNavigation() {
        assertNavigationContains(TwigFileType.INSTANCE, "{{ 'test'|<caret>doctrine_minify_query }}", "Doctrine\\Bundle\\DoctrineBundle\\Twig\\DoctrineExtension::minifyQuery");
        assertNavigationContains(TwigFileType.INSTANCE, "{{ 'test'|<caret>doctrine_pretty_query }}", "SqlFormatter::format");
        assertNavigationContains(TwigFileType.INSTANCE, "{{ 'test'|<caret>json_decode }}", "json_decode");
    }

    public void testTwigExtensionLookupElementPresentable() {
        assertCompletionLookupTailEquals(TwigFileType.INSTANCE, "{{ 'test'|<caret> }}", "doctrine_minify_query", "(query)");
        assertCompletionLookupTailEquals(TwigFileType.INSTANCE, "{{ 'test'|<caret> }}", "doctrine_pretty_query", "()");

        // test parameter strip
        assertCompletionLookupTailEquals(TwigFileType.INSTANCE, "{{ 'test'|<caret> }}", "contextAndEnvironment", "()");
        assertCompletionLookupTailEquals(TwigFileType.INSTANCE, "{{ 'test'|<caret> }}", "contextWithoutEnvironment", "()");
    }

}
