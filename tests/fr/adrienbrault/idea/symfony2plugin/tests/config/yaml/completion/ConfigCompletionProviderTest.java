package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml.completion;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.yaml.completion.ConfigCompletionProvider
 */
public class ConfigCompletionProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testConfigFileCompletion() {
        assertCompletionContains("config.yml", "" +
                "framework:\n" +
                "   b<caret>\n" +
                "   foo: foo\n",
            "base_url"
        );

        assertCompletionContains("config.yml", "" +
                "framework:\n" +
                "   templating:\n" +
                "       a<caret>\n" +
                "       foo: foo\n",
            "assets_base_url"
        );
    }

    public void testConfigInRootFileCompletion() {
        assertCompletionContains("config.yml", "" +
                "framework:\n" +
                "   foo: ~\n" +
                "<caret>",
            "framework", "swiftmailer"
        );
    }
}
