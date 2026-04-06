package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.component;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.DocumentNamespacesParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.InputStream;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DocumentNamespacesParserTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/component";
    }

    public void testParse() throws Exception {
        VirtualFile testFile = myFixture.copyFileToProject("appDevDebugProjectContainer.xml");
        DocumentNamespacesParser entityNamesServiceParser = new DocumentNamespacesParser();
        try (InputStream inputStream = testFile.getInputStream()) {
            entityNamesServiceParser.parser(inputStream, testFile, getProject());
        }
        Map<String, String> map = entityNamesServiceParser.getNamespaceMap();

        assertEquals("\\AcmeProject\\FrontendBundle\\Document", map.get("AcmeProjectFrontendBundle"));
        assertEquals("\\AcmeProject\\ApiBundle\\Document", map.get("AcmeProjectApiBundle"));
        assertEquals("\\AcmeProject\\CoreBundle\\Document", map.get("AcmeProjectCoreBundle"));

        assertEquals("\\AcmeCouchProject\\FrontendBundle\\Document", map.get("AcmeCouchProjectFrontendBundle"));
    }

}
