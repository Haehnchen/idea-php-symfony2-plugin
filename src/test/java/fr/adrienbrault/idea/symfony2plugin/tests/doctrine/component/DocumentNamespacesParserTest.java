package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.component;

import com.intellij.openapi.vfs.VfsUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.DocumentNamespacesParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DocumentNamespacesParserTest extends SymfonyTempCodeInsightFixtureTestCase {

    public void testParse() throws Exception {
        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/component/appDevDebugProjectContainer.xml");
        DocumentNamespacesParser entityNamesServiceParser = new DocumentNamespacesParser();
        entityNamesServiceParser.parser(new FileInputStream(testFile), VfsUtil.findFileByIoFile(testFile, true), getProject());
        Map<String, String> map = entityNamesServiceParser.getNamespaceMap();

        assertEquals("\\AcmeProject\\FrontendBundle\\Document", map.get("AcmeProjectFrontendBundle"));
        assertEquals("\\AcmeProject\\ApiBundle\\Document", map.get("AcmeProjectApiBundle"));
        assertEquals("\\AcmeProject\\CoreBundle\\Document", map.get("AcmeProjectCoreBundle"));

        assertEquals("\\AcmeCouchProject\\FrontendBundle\\Document", map.get("AcmeCouchProjectFrontendBundle"));
    }

}
