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
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    public void testThatInlineVarProvidesClassCompletion() {
        myFixture.copyFileToProject("TwigTemplateCompletionContributorTest.php");

        assertCompletionContains(TwigFileType.INSTANCE, "{# @var bar F<caret> #}", "Foobar");
        assertCompletionContains(TwigFileType.INSTANCE, "{# @var bar MyFoo\\Ca<caret> #}", "Car\\Bike\\Foobar");
    }

    public void testThatInlineVarProvidesClassCompletionAfterUnionSeparator() {
        myFixture.copyFileToProject("TwigTemplateCompletionContributorTest.php");

        assertCompletionContains(TwigFileType.INSTANCE, "{# @var item \\MyFoo\\Car\\Bike\\Foobar|\\MyFoo\\Car\\Bike\\Foo<caret> #}", "Foobar2");
    }

    public void testThatInlineVarProvidesClassCompletionDeprecated() {
        myFixture.copyFileToProject("TwigTemplateCompletionContributorTest.php");

        assertCompletionContains(TwigFileType.INSTANCE, "{# bar F<caret> #}", "Foobar");
    }

    public void testThatTypesTagProvidesClassCompletion() {
        myFixture.copyFileToProject("TwigTemplateCompletionContributorTest.php");

        assertCompletionContains(TwigFileType.INSTANCE, "{% types { bar: 'F<caret>' } %}", "Foobar");
        assertCompletionContains(TwigFileType.INSTANCE, "{% types { bar: 'MyFoo\\Ca<caret>' } %}", "Car\\Bike\\Foobar");
    }

    public void testThatTypesTagProvidesClassCompletionAfterUnionSeparator() {
        myFixture.copyFileToProject("TwigTemplateCompletionContributorTest.php");

        assertCompletionContains(TwigFileType.INSTANCE, "{% types { item: '\\MyFoo\\Car\\Bike\\Foobar|\\MyFoo\\Car\\Bike\\Foo<caret>' } %}", "Foobar2");
    }

    public void testThatTypesTagProvidesClassCompletionWithOptionalMarker() {
        myFixture.copyFileToProject("TwigTemplateCompletionContributorTest.php");

        assertCompletionContains(TwigFileType.INSTANCE, "{% types { bar?: 'F<caret>' } %}", "Foobar");
    }

    public void testThatTypesTagProvidesClassCompletionMultipleVariables() {
        myFixture.copyFileToProject("TwigTemplateCompletionContributorTest.php");

        assertCompletionContains(TwigFileType.INSTANCE, "{% types { foo: 'DateTime', bar: 'F<caret>' } %}", "Foobar");
    }

    public void testThatTypesTagProvidesIncompleteIfStatementCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "\n" +
                "{% types { foobar: '\\\\Foo\\\\Template\\\\Foobar' } %}\n" +
                "{% if<caret> %}\n",
            "if foobar.ready", "if foobar.readyStatus"
        );
    }

    public void testThatTypesTagProvidesIncompleteForStatementCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "\n" +
                "{% types { foobar: '\\\\Foo\\\\Template\\\\Foobar' } %}\n" +
                "{% fo<caret> %}\n",
            "for myfoo in foobar.myfoos", "for date in foobar.dates", "for item in foobar.items"
        );
    }

    public void testIncompleteFormCompletionsUsePrimitiveFormTypeFqnsFromControllerRender() {
        addFormControllerFixture();

        myFixture.addFileToProject("templates/form/completion.html.twig", "{{ form<caret> }}");
        myFixture.configureFromTempProjectFile("templates/form/completion.html.twig");
        myFixture.completeBasic();

        assertContainsElements(myFixture.getLookupElementStrings(), "form_row(form.title)", "form_start(form)");
    }

    public void testThatConstantProvidesCompletionForClassConstant() {
        myFixture.copyFileToProject("TwigFilterExtension.php");

        myFixture.copyFileToProject("TwigTemplateCompletionContributorTest.php");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('<caret>') }}", "CONST_FOO", "FooConst::CAR", "FooEnum::FOOBAR");
        assertCurrentCompletionInserts("FooEnum::FOOBAR", "{{ constant('App\\\\Bike\\\\FooEnum::FOOBAR') }}");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('App\\<caret>') }}", "\\\\Bike\\\\FooConst::CAR", "\\\\Bike\\\\FooEnum::FOOBAR");
        assertCurrentCompletionInserts("\\\\Bike\\\\FooEnum::FOOBAR", "{{ constant('App\\\\\\Bike\\\\FooEnum::FOOBAR') }}");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('App\\\\Bike\\\\<caret>') }}", "FooConst::CAR", "FooEnum::FOOBAR");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('App\\\\Bike\\\\Foo<caret>') }}", "FooEnum::FOOBAR");
        assertCurrentCompletionInserts("FooEnum::FOOBAR", "{{ constant('App\\\\Bike\\\\FooEnum::FOOBAR') }}");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('\\\\App\\\\Bike\\\\Foo<caret>') }}", "FooEnum::FOOBAR");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('BugDemo\\\\<caret>') }}", "NAMESPACED_CONST");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('\\\\BugDemo\\\\<caret>') }}", "NAMESPACED_CONST");
        assertCompletionContains(TwigFileType.INSTANCE, "{# @var suite \\BugDemo\\CardSuite #}\n{{ constant('<caret>', suite) }}", "CLUBS", "SPADES");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('App\\\\Bike\\\\FooConst::C<caret>') }}", "CAR");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('App\\\\Bike\\\\FooEnum::F<caret>') }}", "FOOBAR");
        assertCurrentCompletionInserts("FOOBAR", "{{ constant('App\\\\Bike\\\\FooEnum::FOOBAR') }}");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('\\\\App\\\\Bike\\\\FooEnum::F<caret>') }}", "FOOBAR");

        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ constant('BugDemo\\\\NAMES<caret>') }}", "{{ constant('BugDemo\\\\NAMESPACED_CONST') }}", l -> "NAMESPACED_CONST".equals(l.getLookupString()));
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{# @var suite \\BugDemo\\CardSuite #}\n{{ constant('CL<caret>', suite) }}", "{# @var suite \\BugDemo\\CardSuite #}\n{{ constant('CLUBS', suite) }}", l -> "CLUBS".equals(l.getLookupString()));
    }

    private void assertCurrentCompletionInserts(String lookupString, String expected) {
        var lookup = myFixture.getLookup();
        assertNotNull("Expected an active completion lookup", lookup);
        var item = lookup.getItems().stream()
            .filter(element -> lookupString.equals(element.getLookupString()))
            .findFirst()
            .orElse(null);
        assertNotNull("Missing completion: " + lookupString, item);
        lookup.setCurrentItem(item);
        myFixture.type('\n');
        myFixture.checkResult(expected);
    }

    public void testCompletionForRoutingParameter() {
        myFixture.copyFileToProject("routing.xml");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ path('xml_route', {'<caret>'}) }}", "slug");
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ path('xml_route', {'sl<caret>ug'}) }}", PlatformPatterns.psiElement());
    }

    public void testCompletionForRouteCompareEquals() {
        myFixture.copyFileToProject("routing.xml");

        assertCompletionContains(TwigFileType.INSTANCE, "{% if app.request.attributes.get('_route') == '<caret>' %}", "xml_route");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if app.request.attributes.get('_route') != '<caret>' %}", "xml_route");
    }

    public void testCompletionForRouteCompareStartsWith() {
        myFixture.copyFileToProject("routing.xml");

        assertCompletionContains(TwigFileType.INSTANCE, "{% if app.request.attributes.get('_route') starts with '<caret>' %}", "xml_route");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if app.request.attributes.get('_route') starts with('<caret>') %}", "xml_route");
    }

    public void testCompletionForRouteCompareSameAs() {
        myFixture.copyFileToProject("routing.xml");

        assertCompletionContains(TwigFileType.INSTANCE, "{% if app.request.attributes.get('_route') is same as('<caret>') %}", "xml_route");
    }

    public void testCompletionForRouteCompareInArray() {
        myFixture.copyFileToProject("routing.xml");

        assertCompletionContains(TwigFileType.INSTANCE, "{% if app.request.attributes.get('_route') in ['<caret>'] %}", "xml_route");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if app.request.attributes.get('_route') not in ['<caret>'] %}", "xml_route");
    }

    public void testNoCompletionForRouteCompareWithoutRouteContext() {
        myFixture.copyFileToProject("routing.xml");

        assertCompletionNotContains(TwigFileType.INSTANCE, "{% if app.request.something == '<caret>' %}", "xml_route");
    }

    public void testCompletionForRoutingParameterWithIdentifierHash() {
        myFixture.copyFileToProject("routing.xml");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ path('xml_route', {<caret>}) }}", "slug");
    }

    public void testNavigationForRoutingParameterWithIdentifierHash() {
        myFixture.copyFileToProject("routing.xml");

        assertNavigationMatch(TwigFileType.INSTANCE, "{{ path('xml_route', {sl<caret>ug}) }}", PlatformPatterns.psiElement());
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ path('xml_route', {sl<caret>ug: 'value'}) }}", PlatformPatterns.psiElement());
    }

    public void testCompletionForIncludeWithHashKeys() {
        addIncludeWithKeyTargetTemplate();

        assertCompletionContains(TwigFileType.INSTANCE, "{% include 'include/_key_target.html.twig' with {'<caret>': 'asas'} %}", "asas");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ include('include/_key_target.html.twig', {\"<caret>\": asas}) }}", "doubleKey");
        assertCompletionContains(TwigFileType.INSTANCE, "{% include 'include/_key_target.html.twig' with {plain<caret>Key: asas} %}", "plainKey");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{% include 'include/_key_target.html.twig' with {'provided': 'as<caret>as'} %}", "asas");
    }

    public void testNavigationForIncludeWithHashKeys() {
        addIncludeWithKeyTargetTemplate();

        assertNavigationMatch(TwigFileType.INSTANCE, "{% include 'include/_key_target.html.twig' with {'as<caret>as': 'value'} %}", PlatformPatterns.psiElement().withText("asas"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ include('include/_key_target.html.twig', {\"double<caret>Key\": value}) }}", PlatformPatterns.psiElement().withText("doubleKey"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{% include 'include/_key_target.html.twig' with {plain<caret>Key: value} %}", PlatformPatterns.psiElement().withText("plainKey"));
    }

    public void testCompletionAndNavigationForEmbedWithHashKeys() {
        addIncludeWithKeyTargetTemplate();

        assertCompletionContains(TwigFileType.INSTANCE, "{% embed 'include/_key_target.html.twig' with {\"<caret>\": asas} %}{% endembed %}", "embedKey");
        assertNavigationMatch(TwigFileType.INSTANCE, "{% embed 'include/_key_target.html.twig' with {embed<caret>Key: value} %}{% endembed %}", PlatformPatterns.psiElement().withText("embedKey"));
    }

    public void testCompletionForTwigComponent() {
        myFixture.copyFileToProject("TwigTemplateCompletionContributorTest.php");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ component('<caret>'}) }}", "Alert");
    }

    public void testCompletionForAnonymousIndexComponent() {
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");
        myFixture.addFileToProject("templates/components/Nav/index.html.twig", "<nav></nav>");
        myFixture.addFileToProject("templates/components/Nav/Item.html.twig", "<li></li>");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ component('<caret>'}) }}", "Nav", "Nav:Item");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ component('<caret>'}) }}", "Nav:index");
    }

    public void testInsertHandlerForTwigFunctionWithStringParameter() {
        myFixture.copyFileToProject("TwigTemplateCompletionContributorTest.php");

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

    public void testInlineVarUnionProvidesMemberCompletionFromAllTypes() {
        addTwigUnionFixture();

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{# @var item \\Foo\\Union\\User|\\Foo\\Union\\Admin #}{{ item.<caret> }}",
            "username", "roleName"
        );
    }

    public void testTypesTagUnionProvidesMemberCompletionFromAllTypes() {
        addTwigUnionFixture();

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% types { item: '\\Foo\\Union\\User|\\Foo\\Union\\Admin' } %}{{ item.<caret> }}",
            "username", "roleName"
        );
    }

    public void testInlineVarArrayUnionProvidesForLoopMemberCompletionFromAllTypes() {
        addTwigUnionFixture();

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{# @var items \\Foo\\Union\\User[]|\\Foo\\Union\\Admin[] #}\n" +
                "{% for item in items %}\n" +
                "  {{ item.<caret> }}\n" +
                "{% endfor %}",
            "username", "roleName"
        );
    }

    public void testThatTypeCompletionSupportsMethodCallInPath() {
        addTwigTypePathFixture();

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{# @var root \\Foo\\TypePath\\Root #}{{ root.children.fff('value').<caret> }}",
            "bar", "baz"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{# @var root \\Foo\\TypePath\\Root #}{{ root.getChildren().fff('value').<caret> }}",
            "bar", "baz"
        );
    }

    public void testThatTwigFilterReturnTypeProvidesMethodCompletion() {
        myFixture.copyFileToProject("TwigFilterExtension.php");

        myFixture.copyFileToProject("TwigStringExtension.php");

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ 'Symfony'|u.<caret> }}",
            "truncate", "lower"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% apply u.<caret> %}{% endapply %}",
            "truncate", "lower"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ 'Symfony'|u.truncate(8).<caret> }}",
            "truncate", "lower"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ ustring().<caret> }}",
            "truncate", "lower"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ ustring(ass).<caret> }}",
            "truncate", "lower"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ ustring('aa').<caret> }}",
            "truncate", "lower"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ ustring('aaa').<caret> }}",
            "truncate", "lower"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ ustring(text: 'aaa').<caret> }}",
            "truncate", "lower"
        );
    }

    public void testThatTwigExtensionStringParameterIsPipedToPhpCompletion() {
        myFixture.copyFileToProject("TwigTemplateCompletionContributorTest.php");

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

    private void addFormControllerFixture() {
        myFixture.copyFileToProject("ide-twig.json");
        myFixture.copyFileToProject("FormControllerTemplateVariables.php");
    }

    private void addIncludeWithKeyTargetTemplate() {
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");
        myFixture.addFileToProject(
            "templates/include/_key_target.html.twig",
            "{{ asas }}\n" +
            "{{ doubleKey }}\n" +
            "{{ plainKey }}\n" +
            "{{ embedKey }}"
        );
    }

    private void addTwigTypePathFixture() {
        myFixture.addFileToProject(
            "src/Foo/TypePath/Root.php",
            "<?php\n" +
                "namespace Foo\\TypePath;\n" +
                "class Root { public function getChildren(): Children {} }\n" +
                "class Children { public function fff(string $value): Leaf {} }\n" +
                "class Leaf { public function getBar(): string {} public function getBaz(): string {} }\n"
        );
    }

    private void addTwigUnionFixture() {
        myFixture.addFileToProject(
            "src/Foo/Union/User.php",
            "<?php\n" +
                "namespace Foo\\Union;\n" +
                "class User { public function getUsername(): string {} }\n"
        );

        myFixture.addFileToProject(
            "src/Foo/Union/Admin.php",
            "<?php\n" +
                "namespace Foo\\Union;\n" +
                "class Admin { public function getRoleName(): string {} }\n"
        );
    }

    public void testThatEnumProvidesCompletionForEnumClasses() {
        myFixture.copyFileToProject("TwigFilterExtension.php");
        myFixture.copyFileToProject("TwigTemplateCompletionContributorTest.php");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ enum('<caret>') }}", "FooEnum");
        assertCompletionResultsNotContain("FooConst");
        assertCurrentCompletionInserts("FooEnum", "{{ enum('App\\\\Bike\\\\FooEnum') }}");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ enum('App\\<caret>') }}", "\\\\Bike\\\\FooEnum");
        assertCompletionResultsNotContain("\\\\Bike\\\\FooConst");
        assertCurrentCompletionInserts("\\\\Bike\\\\FooEnum", "{{ enum('App\\\\\\Bike\\\\FooEnum') }}");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ enum('App\\\\Bike\\\\<caret>') }}", "FooEnum");
        assertCompletionResultsNotContain("FooConst");

        // Keep the distinct class-name prefix and its insertion result.
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ enum('App\\\\Bike\\\\Foo<caret>') }}", "{{ enum('App\\\\Bike\\\\FooEnum') }}", l -> "FooEnum".equals(l.getLookupString()));
    }

    public void testThatEnumCasesProvidesCompletionForEnumClasses() {
        myFixture.copyFileToProject("TwigFilterExtension.php");
        myFixture.copyFileToProject("TwigTemplateCompletionContributorTest.php");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ enum_cases('<caret>') }}", "FooEnum");
        assertCompletionResultsNotContain("FooConst");
        assertCurrentCompletionInserts("FooEnum", "{{ enum_cases('App\\\\Bike\\\\FooEnum') }}");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ enum_cases('App\\<caret>') }}", "\\\\Bike\\\\FooEnum");
        assertCompletionResultsNotContain("\\\\Bike\\\\FooConst");
        assertCurrentCompletionInserts("\\\\Bike\\\\FooEnum", "{{ enum_cases('App\\\\\\Bike\\\\FooEnum') }}");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ enum_cases('App\\\\Bike\\\\<caret>') }}", "FooEnum");
        assertCompletionResultsNotContain("FooConst");

        // Keep the distinct class-name prefix and its insertion result.
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ enum_cases('App\\\\Bike\\\\Foo<caret>') }}", "{{ enum_cases('App\\\\Bike\\\\FooEnum') }}", l -> "FooEnum".equals(l.getLookupString()));
    }
}
