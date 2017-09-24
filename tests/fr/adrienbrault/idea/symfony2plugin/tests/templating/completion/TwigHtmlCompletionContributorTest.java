package fr.adrienbrault.idea.symfony2plugin.tests.templating.completion;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.completion.TwigHtmlCompletionContributor
 */
public class TwigHtmlCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("routing.xml");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testThatRouteCompletionInsideHtmlMustProvidePrintPathBlock() {
        assertCompletionResultEquals(
            TwigFileType.INSTANCE,
            "<a href=\"xml_route<caret>\"></a>",
            "<a href=\"{{ path('xml_route', {'slug': 'x'}) }}\"></a>"
        );

        assertCompletionResultEquals(
            TwigFileType.INSTANCE,
            "<form action=\"xml_route<caret>\"></form>",
            "<form action=\"{{ path('xml_route', {'slug': 'x'}) }}\"></form>"
        );
    }
}
