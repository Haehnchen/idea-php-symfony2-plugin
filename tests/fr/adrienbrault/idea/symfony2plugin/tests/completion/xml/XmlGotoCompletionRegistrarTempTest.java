package fr.adrienbrault.idea.symfony2plugin.tests.completion.xml;

import com.intellij.ide.highlighter.XmlFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.completion.xml.XmlGotoCompletionRegistrar
 */
public class XmlGotoCompletionRegistrarTempTest extends SymfonyTempCodeInsightFixtureTestCase {
    public void testThatTemplateInsideRouteDefaultKeyCompletedAndNavigable() {
        createFile("app/Resources/views/foo.html.twig");

        assertCompletionContains(XmlFileType.INSTANCE, "" +
                "    <route id=\"root\" path=\"/wp-admin\">\n" +
                "        <default key=\"template\"><caret></default>\n" +
                "    </route>",
            "foo.html.twig"
        );

        assertNavigationMatch(XmlFileType.INSTANCE, "" +
            "    <route id=\"root\" path=\"/wp-admin\">\n" +
            "        <default key=\"template\">foo.ht<caret>ml.twig</default>\n" +
            "    </route>"
        );
    }
}
