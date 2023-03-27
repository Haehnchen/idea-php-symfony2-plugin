package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.config.yaml.inspection.YamlDuplicateServiceKeyInspection
 */
public class YamlDuplicateParameterKeyInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testDuplicateServiceKeyProvidesWarning() {
        assertLocalInspectionContains("services.yml", "" +
                "parameters:\n" +
                "  fo<caret>o: \n" +
                "  foo: ~\n",
            "Symfony: Duplicate key"
        );

        assertLocalInspectionContains("services.yml", "" +
                "parameters:\n" +
                "  foo: \n" +
                "  f<caret>oo: ~\n",
            "Symfony: Duplicate key"
        );

        assertLocalInspectionNotContains("services.yml", "" +
                "parameters:\n" +
                "  foo1: ~" +
                "  f<caret>oo: ~\n",
            "Symfony: Duplicate key"
        );
    }
}
