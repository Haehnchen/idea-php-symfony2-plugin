package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
 */
public class TwigRoutingCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("routing.yml");
        myFixture.copyFileToProject("routing.xml");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/fixtures";
    }

    public void testTwigPathCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ path('<caret>') }}", "route_foo", "route_bar", "xml_route");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ path(\"<caret>\") }}", "route_foo", "route_bar", "xml_route");

        assertCompletionContains(TwigFileType.INSTANCE, "<a href=\"<caret>\">foo</a>", "route_foo", "route_bar", "xml_route");

        assertCompletionResultEquals(TwigFileType.INSTANCE, "<a href=\"route_foo<caret>\">foo</a>", "<a href=\"{{ path('route_foo', {'var1': 'x', 'var2': 'x', 'var3': 'x'}) }}\">foo</a>");
        assertCompletionResultEquals(TwigFileType.INSTANCE, "<a href=\"route_bar<caret>\">foo</a>", "<a href=\"{{ path('route_bar') }}\">foo</a>");
        assertCompletionResultEquals(TwigFileType.INSTANCE, "<a href=\"xml_route<caret>\">foo</a>", "<a href=\"{{ path('xml_route', {'slug': 'x'}) }}\">foo</a>");

        assertCompletionContains(TwigFileType.INSTANCE, "<form action=\"<caret>\"/>", "route_foo", "route_bar", "xml_route");
        assertCompletionResultEquals(TwigFileType.INSTANCE, "<form action=\"xml_route<caret>\"/>", "<form action=\"{{ path('xml_route', {'slug': 'x'}) }}\"/>");
    }

    public void testTwigPathParameterTailCompletion() {
        assertCompletionLookupTailEquals(TwigFileType.INSTANCE, "{{ path('<caret>') }}", "route_foo", "(var1, var2, var3)");
        assertCompletionLookupTailEquals(TwigFileType.INSTANCE, "{{ path('<caret>') }}", "xml_route", "(slug)");
    }

    /*
    @TODO: not working: pattern changes
    public void testTwigPathParameterCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ path('route_foo', {'<caret>'}) }}", "var1", "var3", "var2");
    }
    */

}
