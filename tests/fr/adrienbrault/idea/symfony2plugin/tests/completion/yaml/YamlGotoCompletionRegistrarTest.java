package fr.adrienbrault.idea.symfony2plugin.tests.completion.yaml;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.completion.yaml.YamlGotoCompletionRegistrar
 */
public class YamlGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("routes.xml");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testThatRouteInsideRouteDefaultKeyCompletedAndNavigable() {
        assertCompletionContains(YAMLFileType.YML, "" +
            "root:\n" +
            "    path: /wp-admin\n" +
            "    defaults:\n" +
            "        route: '<caret>'\n",
            "foo_route"
        );

        assertNavigationMatch(YAMLFileType.YML, "" +
                "root:\n" +
                "    path: /wp-admin\n" +
                "    defaults:\n" +
                "        route: 'foo_<caret>route'\n"
        );
    }

    public void testThatTemplateInsideRouteDefaultKeyCompletedAndNavigable() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        try {
            createDummyFiles("app/Resources/views/foo.html.twig");
        } catch (Exception e) {
            e.printStackTrace();
        }

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
