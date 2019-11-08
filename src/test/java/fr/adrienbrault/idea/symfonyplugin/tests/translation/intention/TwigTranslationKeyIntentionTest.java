package fr.adrienbrault.idea.symfonyplugin.tests.translation.intention;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.translation.intention.TwigTranslationKeyIntention
 */
public class TwigTranslationKeyIntentionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("symfony.de.yml", "Resources/translations/symfony.de.yml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/translation/intention/fixtures";
    }

    public void testThatKeyAlreadyExistsAndProvidesIntentionForOtherDomains() {
        assertIntentionIsAvailable(
            TwigFileType.INSTANCE,
            "{{ 'symfo<caret>ny.great'|trans({}, 'symfony')) }}",
            "Symfony: create translation key"
        );
    }
}
