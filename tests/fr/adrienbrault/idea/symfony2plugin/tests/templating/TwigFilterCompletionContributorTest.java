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


    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor.TagTokenParserCompletionProvider
     */
    public void testTagTokenParserCompletionProvider() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% <caret> %}", "foo_tag");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.TwigHelper#getAfterIsTokenPattern
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor.TwigSimpleTestParametersCompletionProvider
     */
    public void testSimpleTestExtension() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% if foo is <caret> %}", "bar_even");
        assertCompletionContains(TwigFileType.INSTANCE, "{% bar is <caret> %}", "bar_even");
        assertCompletionContains(TwigFileType.INSTANCE, "{% bar is not <caret> %}", "bar_even");

        assertCompletionNotContains(TwigFileType.INSTANCE, "{% bar is<caret> %}", "bar_even");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.TwigHelper#getAfterOperatorPattern
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor.TwigSimpleTestParametersCompletionProvider
     */
    public void testOperatorExtension() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% if foo <caret> %}", "**", "-", "b-or", "b-xor", "ends with", "not", "or", "starts with");

        assertCompletionContains(TwigFileType.INSTANCE, "{% if and foo <caret> %}", "ends with");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if foo is red and blue <caret> %}", "ends with");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if foo is red or blue <caret> %}", "ends with");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if foo is red or 'blue' <caret> %}", "ends with");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if foo is red or \"blue\" <caret> %}", "ends with");

        assertCompletionContains(TwigFileType.INSTANCE, "{% if and foo() <caret> %}", "ends with");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if and foo.0.1.1 <caret> %}", "ends with");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if and foo(111) <caret> %}", "ends with");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if and foo(\"11\") <caret> %}", "ends with");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if and foo('11') <caret> %}", "ends with");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if and foo['11'] <caret> %}", "ends with");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if and foo[11] <caret> %}", "ends with");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if and foo('11')|test <caret> %}", "ends with");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if and foo('11') | test <caret> %}", "ends with");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if and foo('11') | \t test <caret> %}", "ends with");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if and foo[0] | \t test <caret> %}", "ends with");

        assertCompletionNotContains(TwigFileType.INSTANCE, "{% ifa and foo <caret> %}", "ends with");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{% if and foo.<caret> %}", "ends with");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{% if and foo$<caret> %}", "ends with");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{% if and f$oo<caret> %}", "ends with");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{% if and f/oo<caret> %}", "ends with");
    }

}
