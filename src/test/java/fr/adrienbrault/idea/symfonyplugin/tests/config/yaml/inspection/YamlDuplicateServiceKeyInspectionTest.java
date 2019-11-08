package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.config.yaml.inspection.YamlDuplicateServiceKeyInspection
 */
public class YamlDuplicateServiceKeyInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testDuplicateServiceKeyProvidesWarning() {
        assertLocalInspectionContains("routing.yml", "" +
                "services:\n" +
                "  f<caret>oo: \n" +
                "    car: car\n" +
                "  foo: \n" +
                "    car: car \n",
            "Duplicate key"
        );

        assertLocalInspectionContains("routing.yml", "" +
                "services:\n" +
                "  foo: \n" +
                "    car: car \n" +
                "  f<caret>oo: \n" +
                "    car: car",
            "Duplicate key"
        );
    }

}
