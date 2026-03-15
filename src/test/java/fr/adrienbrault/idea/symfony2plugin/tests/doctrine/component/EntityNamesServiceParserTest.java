package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.component;

import com.intellij.openapi.vfs.VfsUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.EntityNamesServiceParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EntityNamesServiceParserTest extends SymfonyTempCodeInsightFixtureTestCase {

    public void testParse() throws Exception {
        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/component/appDevDebugProjectContainer.xml");
        EntityNamesServiceParser entityNamesServiceParser = new EntityNamesServiceParser();
        entityNamesServiceParser.parser(new FileInputStream(testFile), VfsUtil.findFileByIoFile(testFile, true), getProject());
        Map<String, String> map = entityNamesServiceParser.getEntityNameMap();

        assertEquals("\\My\\NiceBundle\\Entity", map.get("MyNiceBundle"));
        assertEquals("\\Your\\TestBundle\\Entity", map.get("YourTestBundle"));
    }

}
