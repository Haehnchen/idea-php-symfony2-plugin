package fr.adrienbrault.idea.symfony2plugin.tests.templating.assets;

import com.intellij.testFramework.UsefulTestCase;
import fr.adrienbrault.idea.symfony2plugin.twig.assets.TwigNamedAssetsServiceParser;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigNamedAssetsServiceParserTest extends UsefulTestCase {

    public void testParse() throws Exception {

        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/assets/appDevDebugProjectContainer.xml");

        TwigNamedAssetsServiceParser parser = new TwigNamedAssetsServiceParser();
        parser.parser(new FileInputStream(testFile));

        Map<String, String[]> namedAssets = parser.getNamedAssets();
        assertNotNull(namedAssets.get("jquery_js"));
        assertNotNull(namedAssets.get("jquery_js2"));

        assertContainsElements(
            Arrays.asList(namedAssets.get("jquery_js2")),
            "../app/Resources/bower/jquery/dist/jquery4.js", "../app/Resources/bower/jquery/dist/jquery3.js"
        );
    }

}
