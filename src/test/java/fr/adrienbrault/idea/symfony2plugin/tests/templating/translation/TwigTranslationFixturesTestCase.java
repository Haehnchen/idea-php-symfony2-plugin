package fr.adrienbrault.idea.symfony2plugin.tests.templating.translation;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
 */
abstract public class TwigTranslationFixturesTestCase extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject("translations/messages.en.yaml", "default_prompt: 'Hello %name% <link>sign in</link>'");
        myFixture.addFileToProject("translations/auth+intl-icu.en.yaml", "auth_prompt: 'Hello {name}! <signin_link>Sign in</signin_link>'");
        myFixture.copyFileToProject("messages.de.yml", "Resources/translations/messages.de.yml");
        myFixture.copyFileToProject("foo.de.yml", "Resources/translations/foo.de.yml");
        myFixture.copyFileToProject("interchange.en.xlf", "interchange.en.xlf");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/translation/fixtures";
    }

}
