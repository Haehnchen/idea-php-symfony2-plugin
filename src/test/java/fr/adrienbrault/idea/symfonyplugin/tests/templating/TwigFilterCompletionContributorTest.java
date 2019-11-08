package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

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

    public void testTwigExtensionFilterCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ 'test'|<caret> }}", "doctrine_minify_query", "doctrine_pretty_query");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ 'test'  |   <caret> }}", "doctrine_minify_query", "doctrine_pretty_query");
        assertCompletionContains(TwigFileType.INSTANCE, "{{     'test'    |       <caret>   }}", "doctrine_minify_query", "doctrine_pretty_query");
    }

    public void testTwigExtensionFilterViaApplyCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% apply <caret> %}foo{% endapply %}", "doctrine_minify_query", "doctrine_pretty_query");
        assertNavigationContains(TwigFileType.INSTANCE, "{% apply doctrine<caret>_minify_query %}foo{% endapply %}", "Doctrine\\Bundle\\DoctrineBundle\\Twig\\DoctrineExtension::minifyQuery");
    }

    public void testTwigExtensionFilterNavigation() {
        assertNavigationContains(TwigFileType.INSTANCE, "{{ 'test'|<caret>doctrine_minify_query }}", "Doctrine\\Bundle\\DoctrineBundle\\Twig\\DoctrineExtension::minifyQuery");
        assertNavigationContains(TwigFileType.INSTANCE, "{{ 'test'|<caret>doctrine_pretty_query }}", "SqlFormatter::format");
        assertNavigationContains(TwigFileType.INSTANCE, "{{ 'test'|<caret>json_decode }}", "my_json_decode");
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
     * @see TwigPattern#getAfterIsTokenPattern
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor.TwigSimpleTestParametersCompletionProvider
     */
    public void testSimpleTestExtension() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% if foo is <caret> %}", "bar_even");
        assertCompletionContains(TwigFileType.INSTANCE, "{% bar is <caret> %}", "bar_even");
        assertCompletionContains(TwigFileType.INSTANCE, "{% bar is not <caret> %}", "bar_even");

        assertCompletionNotContains(TwigFileType.INSTANCE, "{% bar is<caret> %}", "bar_even");
    }

    /**
     * @see TwigPattern#getAfterOperatorPattern
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

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     */
    public void testFunctionExtension() {
        assertNavigationContains(TwigFileType.INSTANCE, "{{ foo<caret>bar() }}", "Doctrine\\Bundle\\DoctrineBundle\\Twig\\DoctrineExtension::foobar");
        assertNavigationContains(TwigFileType.INSTANCE, "{{ json_<caret>bar() }}", "my_json_decode");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ <caret> }}", "foobar");
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ fooba<caret> }}", "{{ foobar() }}");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     */
    public void testMacroFromImport() {
        // skip for _self resolving issue
        if(true) return;

        assertCompletionContains(TwigFileType.INSTANCE, "{% macro foo() %}{% endmacro %}{% from _self import <caret> %}", "foo");
        assertCompletionContains(TwigFileType.INSTANCE, "{% macro foo() %}{% endmacro %}{% from _self import foo %}{{ <caret> }}", "foo");

        assertNavigationMatchWithParent(TwigFileType.INSTANCE, "{% macro foo() %}{% endmacro %}{% from _self import f<caret>oo %}", TwigElementTypes.MACRO_STATEMENT);
        assertNavigationMatchWithParent(TwigFileType.INSTANCE, "{% macro foo() %}{% endmacro %}{% from _self import foo %}{{ fo<caret>o() }}", TwigElementTypes.MACRO_STATEMENT);
        assertNavigationMatchWithParent(TwigFileType.INSTANCE, "{% macro foo() %}{% endmacro %}{% from _self import foo %}{{ fo<caret>o }}", TwigElementTypes.MACRO_STATEMENT);
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     */
    public void testMacroFromImportAlias() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% macro foo() %}{% endmacro %}{% from _self import foo as bar %}{{ <caret> }}", "bar");
        assertNavigationMatchWithParent(TwigFileType.INSTANCE, "{% macro foo() %}{% endmacro %}{% from _self import foo as bar %}{{ b<caret>ar }}", TwigElementTypes.MACRO_STATEMENT);
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     */
    public void testMacroImport() {
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
