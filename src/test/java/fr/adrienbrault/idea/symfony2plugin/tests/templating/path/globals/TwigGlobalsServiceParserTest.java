package fr.adrienbrault.idea.symfony2plugin.tests.templating.path.globals;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalEnum;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalsServiceParser;

import java.io.InputStream;

public class TwigGlobalsServiceParserTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/path/globals";
    }

    public void testParse() throws Exception {
        VirtualFile testFile = myFixture.copyFileToProject("appDevDebugProjectContainer.xml");

        TwigGlobalsServiceParser parser = new TwigGlobalsServiceParser();
        try (InputStream inputStream = testFile.getInputStream()) {
            parser.parser(inputStream, testFile, getProject());
        }

        assertEquals("templating.globals", parser.getTwigGlobals().get("app").getValue());
        assertEquals(TwigGlobalEnum.SERVICE, parser.getTwigGlobals().get("app").getTwigGlobalEnum());
        assertEquals("1.2", parser.getTwigGlobals().get("version").getValue());
    }

}
