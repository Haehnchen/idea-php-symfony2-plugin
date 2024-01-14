package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection;

import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigVariablePathInspection;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigVariablePathInspection
 */
public class TwigVariablePathInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/inspection/fixtures";
    }

    public void testThatUnknownBeforeLeafTypeShouldNotProvideHighlight() {
        assertLocalInspectionNotContains("f.html.twig", "{# @var foo \\Foo\\Bar #} {{ bar.un<caret>known }}", "Field or method not found");
    }

    public void testThatOnlyVariablesWithPublicAccessLevelAreHighlighted() {
        assertLocalInspectionContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.un<caret>known }}", "Field or method not found");
        assertLocalInspectionContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.pri<caret>vate }}", "Field or method not found");
        assertLocalInspectionContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.ap<caret>ple }}", "Field or method not found");
        assertLocalInspectionContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.next.ap<caret>ple  }}", "Field or method not found");

        assertLocalInspectionNotContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.c<caret>ar }}", "Field or method not found");
        assertLocalInspectionNotContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.is<caret>Car }}", "Field or method not found");
        assertLocalInspectionNotContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.has<caret>Hassers }}", "Field or method not found");
        assertLocalInspectionNotContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.ha<caret>ssers }}", "Field or method not found");
        assertLocalInspectionNotContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.pub<caret>lic }}", "Field or method not found");
        assertLocalInspectionNotContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.next.pub<caret>lic }}", "Field or method not found");
        assertLocalInspectionNotContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.getNext.pub<caret>lic }}", "Field or method not found");
        assertLocalInspectionNotContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.FO<caret>O }}", "Field or method not found");
    }

    public void testThatArrayAccessAndIteratorImplementationsDontHighlightAtAll() {
        assertLocalInspectionNotContains("f.html.twig", "{# @var bar \\Foo\\BarArrayAccess #} {{ bar.c<caret>ar }}", "Field or method not found");
        assertLocalInspectionNotContains("f.html.twig", "{# @var bar \\Foo\\BarIterator #} {{ bar.c<caret>ar }}", "Field or method not found");
        assertLocalInspectionNotContains("f.html.twig", "{# @var bar \\Foo\\BarArrayIterator #} {{ bar.c<caret>ar }}", "Field or method not found");
    }

    public void testThatArrayReturnDontHighlight() {
        assertLocalInspectionNotContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.array.f<caret>oo }}", "Field or method not found");

        // 2023.3.4: "Caching disabled due to recursion prevention, please get rid of cyclic dependencies"
        // assertLocalInspectionNotContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.array.foo.fo<caret>o }}", "Field or method not found");
    }
}
