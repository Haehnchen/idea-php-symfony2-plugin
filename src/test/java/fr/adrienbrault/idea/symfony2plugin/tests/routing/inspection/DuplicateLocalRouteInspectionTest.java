package fr.adrienbrault.idea.symfony2plugin.tests.routing.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.routing.inspection.DuplicateLocalRouteInspection
 */
public class DuplicateLocalRouteInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testDuplicateRouteKeyProvidesWarning() {
        assertLocalInspectionContains("routing.yml", "" +
                "foo:\n" +
                "  car: foo\n" +
                "f<caret>oo:\n" +
                "  car: foo\n",
            "Symfony: Duplicate key"
        );

        assertLocalInspectionContains("routing.yml", "" +
                "fo<caret>o:\n" +
                "  car: foo\n" +
                "foo:\n" +
                "  car: foo\n",
            "Symfony: Duplicate key"
        );

        assertLocalInspectionNotContains("routing.yml", "" +
                "foo:\n" +
                "  car: foo\n" +
                "foo<caret>bar:\n" +
                "  car: foo\n" +
                "foo:\n" +
                "  car: foo\n",
            "Symfony: Duplicate key"
        );
    }
}
