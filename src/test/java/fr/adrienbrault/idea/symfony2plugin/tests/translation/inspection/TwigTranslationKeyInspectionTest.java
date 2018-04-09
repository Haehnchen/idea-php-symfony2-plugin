package fr.adrienbrault.idea.symfony2plugin.tests.translation.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

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
            "Missing translation key"
        );

        assertLocalInspectionContains(
            "f.html.twig",
            "{% trans_default_domain symfony %}\n{{ 'f<caret>oo'|trans }}",
            "Missing translation key"
        );
    }

    public void testThatInterpolatedStringsMustNotInspected() {
        assertLocalInspectionNotContains(
            "f.html.twig",
            "{{ 'ti<caret>tle.#{word}'|trans({}, 'symfony')) }}",
            "Missing translation key"
        );
    }

    public void testKnownDomainIsInspected() {
        assertLocalInspectionNotContains(
            "f.html.twig",
            "{{ 'symfon<caret>y.great'|trans({}, 'symfony')) }}",
            "Missing translation key"
        );

        assertLocalInspectionNotContains(
            "f.html.twig",
            "{% trans_default_domain symfony %}\n{{ 'symfon<caret>y.great'|trans }}",
            "Missing translation key"
        );
    }
}
