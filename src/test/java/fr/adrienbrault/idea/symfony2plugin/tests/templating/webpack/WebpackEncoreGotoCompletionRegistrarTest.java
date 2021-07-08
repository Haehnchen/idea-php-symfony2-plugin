package fr.adrienbrault.idea.symfony2plugin.tests.templating.webpack;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.webpack.WebpackEncoreGotoCompletionRegistrar
 */
public class WebpackEncoreGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/webpack/fixtures";
    }

    public void testVisitEntries() {
        myFixture.copyFileToProject("webpack.config.js");
        myFixture.copyFileToProject("entrypoints.json");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ encore_entry_link_tags('<caret>') }}", "foobar");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ encore_entry_script_tags('<caret>') }}", "foobar");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ encore_entry_script_tags('<caret>') }}", "entry_foobar_2");
    }
}
