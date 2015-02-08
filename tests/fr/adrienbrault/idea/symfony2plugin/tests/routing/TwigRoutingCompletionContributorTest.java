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
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testTwigPathCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ path('<caret>') }}", "route_foo", "route_bar");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ path(\"<caret>\") }}", "route_foo", "route_bar");

        assertCompletionContains(TwigFileType.INSTANCE, "<a href=\"<caret>\">foo</a>", "route_foo", "route_bar");

        assertCompletionResultEquals(TwigFileType.INSTANCE, "<a href=\"route_foo<caret>\">foo</a>", "<a href=\"{{ path('route_foo', {'var3': 'x', 'var1': 'x', 'var2': 'x'}) }}\">foo</a>");
        assertCompletionResultEquals(TwigFileType.INSTANCE, "<a href=\"route_bar<caret>\">foo</a>", "<a href=\"{{ path('route_bar') }}\">foo</a>");
    }

}
