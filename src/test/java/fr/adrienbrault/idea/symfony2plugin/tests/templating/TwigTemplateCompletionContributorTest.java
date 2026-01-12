package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
 */
public class TwigTemplateCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("TwigTemplateCompletionContributorTest.php");
        myFixture.copyFileToProject("routing.xml");
        myFixture.copyFileToProject("TwigFilterExtension.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    public void testThatInlineVarProvidesClassCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "{# @var bar F<caret> #}", "Foobar");
        assertCompletionContains(TwigFileType.INSTANCE, "{# @var bar MyFoo\\Ca<caret> #}", "Car\\Bike\\Foobar");
    }

    public void testThatInlineVarProvidesClassCompletionDeprecated() {
        assertCompletionContains(TwigFileType.INSTANCE, "{# bar F<caret> #}", "Foobar");
    }

    public void testThatConstantProvidesCompletionForClassConstant() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('<caret>') }}", "CONST_FOO");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('<caret>') }}", "FooConst::CAR", "FooEnum::FOOBAR");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('App\\<caret>') }}", "\\\\Bike\\\\FooConst::CAR", "\\\\Bike\\\\FooEnum::FOOBAR");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('App\\\\Bike\\\\<caret>') }}", "FooConst::CAR", "FooEnum::FOOBAR");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('App\\\\Bike\\\\Foo<caret>') }}", "FooEnum::FOOBAR");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('\\\\App\\\\Bike\\\\Foo<caret>') }}", "FooEnum::FOOBAR");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('App\\\\Bike\\\\FooConst::C<caret>') }}", "CAR");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('App\\\\Bike\\\\FooEnum::F<caret>') }}", "FOOBAR");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('\\\\App\\\\Bike\\\\FooEnum::F<caret>') }}", "FOOBAR");

        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ constant('<caret>') }}", "{{ constant('App\\\\Bike\\\\FooEnum::FOOBAR') }}", l -> "FooEnum::FOOBAR".equals(l.getLookupString()));
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ constant('App\\<caret>') }}", "{{ constant('App\\\\\\Bike\\\\FooEnum::FOOBAR') }}", l -> "\\\\Bike\\\\FooEnum::FOOBAR".equals(l.getLookupString()));
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ constant('App\\\\Bike\\\\Foo<caret>') }}", "{{ constant('App\\\\Bike\\\\FooEnum::FOOBAR') }}", l -> "FooEnum::FOOBAR".equals(l.getLookupString()));
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ constant('App\\\\Bike\\\\FooEnum::F<caret>') }}", "{{ constant('App\\\\Bike\\\\FooEnum::FOOBAR') }}", l -> "FOOBAR".equals(l.getLookupString()));
    }

    public void testCompletionForRoutingParameter() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ path('xml_route', {'<caret>'}) }}", "slug");
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ path('xml_route', {'sl<caret>ug'}) }}", PlatformPatterns.psiElement());
    }

    public void testCompletionForTwigComponent() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ component('<caret>'}) }}", "Alert");
    }

    public void testInsertHandlerForTwigFunctionWithStringParameter() {
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ a_test<caret> }}", "{{ a_test('') }}");
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ b_test<caret> }}", "{{ b_test('') }}");
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ c_test<caret> }}", "{{ c_test('') }}");
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ d_test<caret> }}", "{{ d_test() }}");
    }

    public void testThatMacroImportProvidesCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "" +
                "{% import _self as foobar1 %}\n" +
                "{% macro foobar(foobar) %}{% endmacro %}\n" +
                "{{ foobar1.<caret> }}\n",
            "foobar"
        );
    }

    public void testThatIncompleteIfStatementIsCompletedWithVariables() {
        assertCompletionContains(TwigFileType.INSTANCE, "\n" +
                "{# @var \\Foo\\Template\\Foobar foobar #}\n" +
                "{% if<caret> %}\n",
            "if foobar.ready", "if foobar.readyStatus"
        );
    }

    public void testThatIncompleteForStatementIsCompletedWithVariables() {
        assertCompletionContains(TwigFileType.INSTANCE, "\n" +
                "{# @var \\Foo\\Template\\Foobar foobar #}\n" +
                "{% fo<caret> %}\n",
            "for myfoo in foobar.myfoos", "for date in foobar.dates", "for item in foobar.items"
        );
    }

    public void testThatTwigMethodStringParameterIsPipedToPhpCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "\n" +
                "{# @var \\Symfony\\Component\\HttpFoundation\\Request request #}\n" +
                "{% request.isMethod('<caret>') %}\n",
            "GET", "POST"
        );
    }

    public void testThatTwigExtensionStringParameterIsPipedToPhpCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "\n" +
                "{{ 'aaa'|request_filter('<caret>') }}\n",
            "GET", "POST"
        );

        assertCompletionContains(TwigFileType.INSTANCE, "\n" +
                "{% apply request_filter('<caret>') %}{% endapply %}\n",
            "GET", "POST"
        );

        assertCompletionContains(TwigFileType.INSTANCE, "\n" +
                "{{ request_function('<caret>') }}\n",
            "GET", "POST"
        );

        assertCompletionNotContains(TwigFileType.INSTANCE, "\n" +
                "{{ test.request_function('<caret>') }}\n",
            "GET", "POST"
        );
    }

    public void testSelfMacroImport() {
        assertCompletionContains(TwigFileType.INSTANCE, "\n" +
                "{% macro foobar(name) %}{% endmacro %}\n" +
                "{{ _self.f<caret>o }}",
            "foobar"
        );
    }

    public void testThatEnumProvidesCompletionForEnumClasses() {
        // Test basic completion - enum should be available
        assertCompletionContains(TwigFileType.INSTANCE, "{{ enum('<caret>') }}", "FooEnum");

        // Ensure non-enum classes are NOT included
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ enum('<caret>') }}", "FooConst");

        // Test namespace-based completion
        assertCompletionContains(TwigFileType.INSTANCE, "{{ enum('App\\<caret>') }}", "\\\\Bike\\\\FooEnum");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ enum('App\\<caret>') }}", "\\\\Bike\\\\FooConst");

        // Test sub-namespace completion
        assertCompletionContains(TwigFileType.INSTANCE, "{{ enum('App\\\\Bike\\\\<caret>') }}", "FooEnum");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ enum('App\\\\Bike\\\\<caret>') }}", "FooConst");

        // Test that the insert handler properly escapes backslashes
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ enum('<caret>') }}", "{{ enum('App\\\\Bike\\\\FooEnum') }}", l -> "FooEnum".equals(l.getLookupString()));
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ enum('App\\<caret>') }}", "{{ enum('App\\\\\\Bike\\\\FooEnum') }}", l -> "\\\\Bike\\\\FooEnum".equals(l.getLookupString()));
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ enum('App\\\\Bike\\\\Foo<caret>') }}", "{{ enum('App\\\\Bike\\\\FooEnum') }}", l -> "FooEnum".equals(l.getLookupString()));
    }

    public void testThatEnumCasesProvidesCompletionForEnumClasses() {
        // Test basic completion - enum should be available
        assertCompletionContains(TwigFileType.INSTANCE, "{{ enum_cases('<caret>') }}", "FooEnum");

        // Ensure non-enum classes are NOT included
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ enum_cases('<caret>') }}", "FooConst");

        // Test namespace-based completion
        assertCompletionContains(TwigFileType.INSTANCE, "{{ enum_cases('App\\<caret>') }}", "\\\\Bike\\\\FooEnum");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ enum_cases('App\\<caret>') }}", "\\\\Bike\\\\FooConst");

        // Test sub-namespace completion
        assertCompletionContains(TwigFileType.INSTANCE, "{{ enum_cases('App\\\\Bike\\\\<caret>') }}", "FooEnum");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ enum_cases('App\\\\Bike\\\\<caret>') }}", "FooConst");

        // Test that the insert handler properly escapes backslashes
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ enum_cases('<caret>') }}", "{{ enum_cases('App\\\\Bike\\\\FooEnum') }}", l -> "FooEnum".equals(l.getLookupString()));
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ enum_cases('App\\<caret>') }}", "{{ enum_cases('App\\\\\\Bike\\\\FooEnum') }}", l -> "\\\\Bike\\\\FooEnum".equals(l.getLookupString()));
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ enum_cases('App\\\\Bike\\\\Foo<caret>') }}", "{{ enum_cases('App\\\\Bike\\\\FooEnum') }}", l -> "FooEnum".equals(l.getLookupString()));
    }
}
