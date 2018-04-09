package fr.adrienbrault.idea.symfony2plugin.tests.translation.intention;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.translation.intention.TwigTranslationKeyIntention
 */
public class TwigTranslationKeyIntentionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("symfony.de.yml", "Resources/translations/symfony.de.yml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/intention/fixtures";
    }

    public void testThatKeyAlreadyExistsAndProvidesIntentionForOtherDomains() {
        assertIntentionIsAvailable(
            TwigFileType.INSTANCE,
            "{{ 'symfo<caret>ny.great'|trans({}, 'symfony')) }}",
            "Symfony: create translation key"
        );
    }
}
