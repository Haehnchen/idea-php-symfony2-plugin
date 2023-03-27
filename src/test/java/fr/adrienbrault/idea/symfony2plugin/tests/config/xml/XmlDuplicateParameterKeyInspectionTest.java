package fr.adrienbrault.idea.symfony2plugin.tests.config.xml;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.xml.inspection.XmlDuplicateParameterKeyInspection
 */
public class XmlDuplicateParameterKeyInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testDuplicateParameterKey() {
        assertLocalInspectionContains("service.xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                "<container>\n" +
                "\n" +
                "    <parameters>\n" +
                "        <parameter key=\"mailer.transport\">foo</parameter>\n" +
                "        <parameter key=\"maile<caret>r.transport\">foo1</parameter>\n" +
                "    </parameters>\n" +
                "\n" +
                "</container>",
            "Symfony: Duplicate Key"
        );
    }
}
