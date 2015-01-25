package fr.adrienbrault.idea.symfony2plugin.tests.templating.assets;

import fr.adrienbrault.idea.symfony2plugin.templating.assets.TwigNamedAssetsServiceParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigNamedAssetsServiceParserTest extends Assert {

    @Test
    public void testParse() throws Exception {

        File testFile = new File(this.getClass().getResource("appDevDebugProjectContainer.xml").getFile());

        TwigNamedAssetsServiceParser parser = new TwigNamedAssetsServiceParser();
        parser.parser(testFile);

        Map<String, String[]> namedAssets = parser.getNamedAssets();
        assertNotNull(namedAssets.get("jquery_js"));
        assertNotNull(namedAssets.get("jquery_js2"));
    }

}
