package fr.adrienbrault.idea.symfony2plugin.tests.templating.completion;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.completion.TwigHtmlCompletionContributor
 */
public class TwigHtmlCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("routing.xml");
        myFixture.copyFileToProject("symfony.de.xlf");
        myFixture.copyFileToProject("messages.de.xlf");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/completion/fixtures";
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

    public void testThatTranslationCompletionInsideHtmlMustProvideTransFilter() {
        assertCompletionResultEquals(
            "test.html.twig",
            "<a>messages_foobar<caret></a>",
            "<a>{{ 'messages_foobar'|trans }}</a>"
        );

        assertCompletionResultEquals(
            "test.html.twig",
            "<input value=\"messages_foobar<caret>\">",
            "<input value=\"{{ 'messages_foobar'|trans }}\">"
        );

        assertCompletionResultEquals(
            "test.html.twig",
            "{{ 'foo'|trans(null, 'symfony') }}<input value=\"symfony_foobar<caret>\">",
            "{{ 'foo'|trans(null, 'symfony') }}<input value=\"{{ 'symfony_foobar'|trans({}, 'symfony') }}\">"
        );
    }

    /**
     * Test that prop completion pattern works for twig component tags.
     */
    public void testThatPropCompletionPatternWorksForTwigComponentTag() {
        myFixture.copyFileToProject("twig_component.yaml", "config/packages/twig_component.yaml");
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");
        myFixture.copyFileToProject("PropsAlert.html.twig", "templates/components/PropsAlert.html.twig");

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "<twig:PropsAlert ic<caret> />",
            "icon"
        );
        assertCompletionNotContains(
            TwigFileType.INSTANCE,
            "<twig:PropsAlert ic<caret> />",
            "type", "message"
        );
    }

    /**
     * Test that prop completion provides template props from {% props %} definitions.
     */
    public void testThatPropCompletionProvidesTemplatePropsForTwigComponentTag() {
        // Setup configuration files
        myFixture.copyFileToProject("twig_component.yaml", "config/packages/twig_component.yaml");
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");
        myFixture.copyFileToProject("PropsAlert.html.twig", "templates/components/PropsAlert.html.twig");

        // Test completion for props in twig component tag attribute position
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "<twig:PropsAlert <caret> />",
            "icon", "type", "message"
        );
    }

    /**
     * Test that prop completion works with colon prefix for named blocks.
     */
    public void testThatPropCompletionProvidesColonPrefixedProps() {
        // Setup configuration files
        myFixture.copyFileToProject("twig_component.yaml", "config/packages/twig_component.yaml");
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");
        myFixture.copyFileToProject("PropsAlert.html.twig", "templates/components/PropsAlert.html.twig");

        // Test completion with colon prefix
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "<twig:PropsAlert :<caret> />",
            ":icon", ":type", ":message"
        );
    }

}
