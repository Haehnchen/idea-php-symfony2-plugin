package fr.adrienbrault.idea.symfony2plugin.tests.templating.completion;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.completion.TwigHtmlCompletionContributor
 */
public class TwigHtmlBlockCompletionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        // PHP component class with #[AsTwigComponent]
        myFixture.copyFileToProject("BlockCompletionAlert.php", "src/BlockCompletionAlert.php");

        // Twig component configuration
        myFixture.copyFileToProject("twig_component.yaml", "config/packages/twig_component.yaml");

        // Template namespace configuration
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");

        // Component template with blocks
        myFixture.copyFileToProject("Alert.html.twig", "templates/components/Alert.html.twig");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/completion/fixtures";
    }

    public void testBlockTagCompletionInsideComponentRoot() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "<twig:Alert><caret></twig:Alert>",
            "twig:block name=\"message\"",
            "twig:block name=\"actions\""
        );
    }

    public void testBlockTagCompletionInsideComponentRootWhenTypingTagName() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "<twig:Alert><t<caret></twig:Alert>",
            "twig:block name=\"message\"",
            "twig:block name=\"actions\""
        );
    }

    public void testBlockTagCompletionNotInsideBlockNameAttribute() {
        assertCompletionNotContains(
            TwigFileType.INSTANCE,
            "<twig:Alert><twig:block name=\"<caret>\"></twig:block></twig:Alert>",
            "twig:block name=\"message\"",
            "twig:block name=\"actions\""
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "<twig:Alert><twig:block name=\"<caret>\"></twig:block></twig:Alert>",
            "message",
            "actions"
        );
    }

    public void testBlockTagCompletionNotInsideNestedTag() {
        assertCompletionNotContains(
            TwigFileType.INSTANCE,
            "<twig:Alert><div><caret></div></twig:Alert>",
            "twig:block name=\"message\"",
            "twig:block name=\"actions\""
        );

        assertCompletionNotContains(
            TwigFileType.INSTANCE,
            "<twig:Alert><div><t<caret></div></twig:Alert>",
            "twig:block name=\"message\"",
            "twig:block name=\"actions\""
        );
    }
}
