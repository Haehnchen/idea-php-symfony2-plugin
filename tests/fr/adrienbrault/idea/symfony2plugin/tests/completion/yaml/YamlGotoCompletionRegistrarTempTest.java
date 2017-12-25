package fr.adrienbrault.idea.symfony2plugin.tests.completion.yaml;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.completion.yaml.YamlGotoCompletionRegistrar
 */
public class YamlGotoCompletionRegistrarTempTest extends SymfonyTempCodeInsightFixtureTestCase {
    public void testThatTemplateInsideRouteDefaultKeyCompletedAndNavigable() {
        createFile("app/Resources/views/foo.html.twig");

        assertCompletionContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    path: /wp-admin\n" +
                "    defaults:\n" +
                "        template: '<caret>'\n",
            "foo.html.twig"
        );

        assertNavigationMatch(YAMLFileType.YML, "" +
            "root:\n" +
            "    path: /wp-admin\n" +
            "    defaults:\n" +
            "        template: 'foo.ht<caret>ml.twig'\n"
        );
    }
}
