package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigVariableDeprecatedInspection
 */
public class TwigVariableDeprecatedInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testThatDeprecatedMethodIsHighlighted() {
        assertLocalInspectionContains("f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.next.dep<caret>recated  }}", "Method 'Bar::getDeprecated' is deprecated");
    }
}
