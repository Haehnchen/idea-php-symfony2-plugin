package fr.adrienbrault.idea.symfony2plugin.tests.translation.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.translation.PhpTranslationKeyInspection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.translation.inspection.TwigTranslationKeyInspection
 */
public class TwigTranslationKeyInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
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
            "{{ 'f<caret>oo'|trans({}, 'symfony')) }}",
            PhpTranslationKeyInspection.MESSAGE
        );

        assertLocalInspectionContains(
            "f.html.twig",
            "{% trans_default_domain symfony %}\n{{ 'f<caret>oo'|trans }}",
            PhpTranslationKeyInspection.MESSAGE
        );
    }

    public void testThatInterpolatedStringsMustNotInspected() {
        assertLocalInspectionNotContains(
            "f.html.twig",
            "{{ 'ti<caret>tle.#{word}'|trans({}, 'symfony')) }}",
            PhpTranslationKeyInspection.MESSAGE
        );
    }

    public void testKnownDomainIsInspected() {
        assertLocalInspectionNotContains(
            "f.html.twig",
            "{{ 'symfon<caret>y.great'|trans({}, 'symfony')) }}",
            PhpTranslationKeyInspection.MESSAGE
        );

        assertLocalInspectionNotContains(
            "f.html.twig",
            "{% trans_default_domain symfony %}\n{{ 'symfon<caret>y.great'|trans }}",
            PhpTranslationKeyInspection.MESSAGE
        );
    }
}
