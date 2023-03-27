package fr.adrienbrault.idea.symfony2plugin.tests.config.xml.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.xml.inspection.XmlDuplicateServiceKeyInspection
 */
public class XmlDuplicateServiceKeyInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testDuplicateParameterKey() {
        assertLocalInspectionContains("service.xml","" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service id=\"foo<caret>bar\"/>\n" +
                "        <service id=\"foobar\"/>\n" +
                "    </services>\n" +
                "</container>",
            "Symfony: Duplicate Key"
        );
    }
}
