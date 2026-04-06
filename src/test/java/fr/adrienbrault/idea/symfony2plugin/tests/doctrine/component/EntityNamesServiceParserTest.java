package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.component;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.EntityNamesServiceParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.InputStream;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EntityNamesServiceParserTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/component";
    }

    public void testParse() throws Exception {
        VirtualFile testFile = myFixture.copyFileToProject("appDevDebugProjectContainer.xml");
        EntityNamesServiceParser entityNamesServiceParser = new EntityNamesServiceParser();
        try (InputStream inputStream = testFile.getInputStream()) {
            entityNamesServiceParser.parser(inputStream, testFile, getProject());
        }
        Map<String, String> map = entityNamesServiceParser.getEntityNameMap();

        assertEquals("\\My\\NiceBundle\\Entity", map.get("MyNiceBundle"));
        assertEquals("\\Your\\TestBundle\\Entity", map.get("YourTestBundle"));
    }

}
