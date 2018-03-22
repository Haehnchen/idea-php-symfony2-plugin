package fr.adrienbrault.idea.symfony2plugin.tests.translation.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTranslationDomainInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("symfony.de.yml", "Resources/translations/symfony.de.yml");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testUnknownDomainIsInspected() {
        assertLocalInspectionContains(
            "f.html.twig",
            "{{ 'foo'|trans({}, 'UNK<caret>NOWN')) }}",
            "Missing translation domain"
        );
    }

    public void testKnownDomainIsInspected() {
        assertLocalInspectionNotContains(
            "f.html.twig",
            "{{ 'foo'|trans({}, 'sy<caret>mfony')) }}",
            "Missing translation domain"
        );
    }
}
