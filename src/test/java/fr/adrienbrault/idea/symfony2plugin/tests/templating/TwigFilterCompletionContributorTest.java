package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Arrays;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigTemplateCompletionContributor
 */
public class TwigFilterCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("TwigFilterExtension.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    public void testTwigExtensionFilterCompletionAndNavigation() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ 'test'|<caret> }}", "doctrine_minify_query", "doctrine_pretty_query");

        // All tail texts belong to the same completion result.
        assertCurrentCompletionTailEquals("doctrine_minify_query", "(query)");
        assertCurrentCompletionTailEquals("doctrine_pretty_query", "()");
        assertCurrentCompletionTailEquals("contextAndEnvironment", "()");
        assertCurrentCompletionTailEquals("contextWithoutEnvironment", "()");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ 'test'  |   <caret> }}", "doctrine_minify_query", "doctrine_pretty_query");
        assertCompletionContains(TwigFileType.INSTANCE, "{{     'test'    |       <caret>   }}", "doctrine_minify_query", "doctrine_pretty_query");

        assertCompletionContains(TwigFileType.INSTANCE, "{% apply <caret> %}foo{% endapply %}", "doctrine_minify_query", "doctrine_pretty_query");
        assertNavigationContains(TwigFileType.INSTANCE, "{% apply doctrine<caret>_minify_query %}foo{% endapply %}", "Doctrine\\Bundle\\DoctrineBundle\\Twig\\DoctrineExtension::minifyQuery");

        assertNavigationContains(TwigFileType.INSTANCE, "{{ 'test'|<caret>doctrine_minify_query }}", "Doctrine\\Bundle\\DoctrineBundle\\Twig\\DoctrineExtension::minifyQuery");
        assertNavigationContains(TwigFileType.INSTANCE, "{{ 'test'|<caret>doctrine_pretty_query }}", "SqlFormatter::format");
        assertNavigationContains(TwigFileType.INSTANCE, "{{ 'test'|<caret>json_decode }}", "my_json_decode");
    }

    private void assertCurrentCompletionTailEquals(String lookupString, String tailText) {
        var element = Arrays.stream(myFixture.getLookupElements())
            .filter(item -> lookupString.equals(item.getLookupString()))
            .findFirst().orElse(null);
        assertNotNull("Missing completion: " + lookupString, element);
        LookupElementPresentation presentation = new LookupElementPresentation();
        element.renderElement(presentation);
        assertEquals(lookupString, tailText, presentation.getTailText());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor.TagTokenParserCompletionProvider
     */
    public void testTagAndSimpleTestCompletions() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% <caret> %}", "foo_tag");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if foo is <caret> %}", "bar_even");
        assertCompletionContains(TwigFileType.INSTANCE, "{% bar is <caret> %}", "bar_even");
        assertCompletionContains(TwigFileType.INSTANCE, "{% bar is not <caret> %}", "bar_even");

        assertCompletionNotContains(TwigFileType.INSTANCE, "{% bar is<caret> %}", "bar_even");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{% if foo is <caret> %}", "b-and");
    }

    /**
     * @see TwigPattern#getAfterOperatorPattern
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor.TwigSimpleTestParametersCompletionProvider
     */
    public void testOperatorExtension() {
        // Syntax variants are covered by TwigPatternTest.testAfterOperatorPatternForCompletionContexts.
        assertCompletionContains(TwigFileType.INSTANCE, "{% if foo <caret> %}",
            "**", "-", "b-or", "b-xor", "ends with", "not", "or", "starts with", "b-and", "expression_not", "? :");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if and foo[0] | \t test <caret> %}", "ends with");

        // Retain end-to-end exclusions, including completion prefix filtering in malformed expressions.
        assertCompletionNotContains(TwigFileType.INSTANCE, "{% ifa and foo <caret> %}", "ends with");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{% if and foo.<caret> %}", "ends with");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{% if and foo$<caret> %}", "ends with");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{% if and f$oo<caret> %}", "ends with");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{% if and f/oo<caret> %}", "ends with");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     */
    public void testFunctionAndMacroCompletions() {
        assertNavigationContains(TwigFileType.INSTANCE, "{{ foo<caret>bar() }}", "Doctrine\\Bundle\\DoctrineBundle\\Twig\\DoctrineExtension::foobar");
        assertNavigationContains(TwigFileType.INSTANCE, "{{ json_<caret>bar() }}", "my_json_decode");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ <caret> }}", "foobar");
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ fooba<caret> }}", "{{ foobar() }}");

        assertCompletionContains(TwigFileType.INSTANCE, "{% macro foo() %}{% endmacro %}{% from _self import foo as bar %}{{ <caret> }}", "bar");
        assertNavigationMatchWithParent(TwigFileType.INSTANCE, "{% macro foo() %}{% endmacro %}{% from _self import foo as bar %}{{ b<caret>ar }}", TwigElementTypes.MACRO_STATEMENT);

        assertCompletionContains(TwigFileType.INSTANCE, "{% macro foo() %}{% endmacro %}{% import _self as bar %}{{ <caret> }}", "bar.foo");
        assertNavigationMatchWithParent(TwigFileType.INSTANCE, "{% macro foo() %}{% endmacro %}{% import _self as bar %}{{ bar.f<caret>oo }}", TwigElementTypes.MACRO_STATEMENT);
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateGoToDeclarationHandler
     */
    public void testControllerReferences() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ controller('<caret>') }}", "FooBundle:Foo:bar");
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ controller('FooBundl<caret>e:Foo:bar') }}", PlatformPatterns.psiElement(Method.class).withName("barAction"));

        assertCompletionContains(TwigFileType.INSTANCE, "{{ controller(\"<caret>\") }}", "FooBundle:Foo:bar");
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ controller(\"FooBundl<caret>e:Foo:bar\") }}", PlatformPatterns.psiElement(Method.class).withName("barAction"));

        assertCompletionContains(TwigFileType.INSTANCE, "{{ render(controller('<caret>')) }}", "FooBundle:Foo:bar");
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ render(controller('FooBundl<caret>e:Foo:bar')) }}", PlatformPatterns.psiElement(Method.class).withName("barAction"));

        assertCompletionContains(TwigFileType.INSTANCE, "{% render(controller('<caret>')) %}", "FooBundle:Foo:bar");
        assertNavigationMatch(TwigFileType.INSTANCE, "{% render(controller('FooBundl<caret>e:Foo:bar')) %}", PlatformPatterns.psiElement(Method.class).withName("barAction"));

        assertCompletionContains(TwigFileType.INSTANCE, "{% render '<caret>' %}", "FooBundle:Foo:bar");
        assertNavigationMatch(TwigFileType.INSTANCE, "{% render 'FooBundl<caret>e:Foo:bar' %}", PlatformPatterns.psiElement(Method.class).withName("barAction"));

        assertCompletionContains(TwigFileType.INSTANCE, "{% render \"<caret>\" %}", "FooBundle:Foo:bar");
        assertNavigationMatch(TwigFileType.INSTANCE, "{% render \"FooBundl<caret>e:Foo:bar\" %}", PlatformPatterns.psiElement(Method.class).withName("barAction"));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateGoToDeclarationHandler
     */
    public void testSetTagIsAvailableForFunctionReferences() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% set = <caret> %}", "json_bar");
        assertNavigationMatch(TwigFileType.INSTANCE, "{% set = json_<caret>bar() %}", PlatformPatterns.psiElement(Function.class));
    }
}
