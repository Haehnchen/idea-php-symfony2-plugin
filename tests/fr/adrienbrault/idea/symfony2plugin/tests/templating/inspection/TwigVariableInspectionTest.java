package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection;

import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigVariablePathInspection;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigVariablePathInspection
 */
public class TwigVariableInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testThatUnknownBeforeLeafTypeShouldNotProvideHighlight() {
        assertLocalInspectionContainsNotContains("f.html.twig", "{# @var foo \\Foo\\Bar #} {{ bar.un<caret>known }}", "Field or method not found");
    }

    public void testThatOnlyVariablesWithPublicAccessLevelAreHighlighted() {
        assertLocalInspectionContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.un<caret>known }}", "Field or method not found");
        assertLocalInspectionContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.pri<caret>vate }}", "Field or method not found");
        assertLocalInspectionContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.ap<caret>ple }}", "Field or method not found");
        assertLocalInspectionContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.next.ap<caret>ple  }}", "Field or method not found");

        assertLocalInspectionContainsNotContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.c<caret>ar }}", "Field or method not found");
        assertLocalInspectionContainsNotContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.is<caret>Car }}", "Field or method not found");
        assertLocalInspectionContainsNotContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.pub<caret>lic }}", "Field or method not found");
        assertLocalInspectionContainsNotContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.next.pub<caret>lic }}", "Field or method not found");
        assertLocalInspectionContainsNotContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.getNext.pub<caret>lic }}", "Field or method not found");
        assertLocalInspectionContainsNotContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.FO<caret>O }}", "Field or method not found");
    }

    public void testThatArrayAccessAndIteratorImplementationsDontHighlightAtAll() {
        assertLocalInspectionContainsNotContains("f.html.twig", "{# @var bar \\Foo\\BarArrayAccess #} {{ bar.c<caret>ar }}", "Field or method not found");
        assertLocalInspectionContainsNotContains("f.html.twig", "{# @var bar \\Foo\\BarIterator #} {{ bar.c<caret>ar }}", "Field or method not found");
        assertLocalInspectionContainsNotContains("f.html.twig", "{# @var bar \\Foo\\BarArrayIterator #} {{ bar.c<caret>ar }}", "Field or method not found");
    }
}
