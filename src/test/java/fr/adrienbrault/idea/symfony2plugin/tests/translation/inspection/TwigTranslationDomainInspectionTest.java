package fr.adrienbrault.idea.symfony2plugin.tests.translation.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TwigTranslationDomainInspection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTranslationDomainInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("symfony.de.yml", "Resources/translations/symfony.de.yml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/inspection/fixtures";
    }

    public void testUnknownDomainIsInspected() {
        assertLocalInspectionContains(
            "f.html.twig",
            "{{ 'foo'|trans({}, 'UNK<caret>NOWN')) }}",
            TwigTranslationDomainInspection.MESSAGE
        );
    }

    public void testKnownDomainIsInspected() {
        assertLocalInspectionNotContains(
            "f.html.twig",
            "{{ 'foo'|trans({}, 'sy<caret>mfony')) }}",
            TwigTranslationDomainInspection.MESSAGE
        );
    }
}
