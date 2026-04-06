package fr.adrienbrault.idea.symfony2plugin.tests.templating.assets;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.twig.assets.TwigNamedAssetsServiceParser;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigNamedAssetsServiceParserTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/assets";
    }

    public void testParse() throws Exception {
        VirtualFile testFile = myFixture.copyFileToProject("appDevDebugProjectContainer.xml");

        TwigNamedAssetsServiceParser parser = new TwigNamedAssetsServiceParser();
        try (InputStream inputStream = testFile.getInputStream()) {
            parser.parser(inputStream, testFile, getProject());
        }

        Map<String, String[]> namedAssets = parser.getNamedAssets();
        assertNotNull(namedAssets.get("jquery_js"));
        assertNotNull(namedAssets.get("jquery_js2"));

        assertContainsElements(
            Arrays.asList(namedAssets.get("jquery_js2")),
            "../app/Resources/bower/jquery/dist/jquery4.js", "../app/Resources/bower/jquery/dist/jquery3.js"
        );
    }

}
