package fr.adrienbrault.idea.symfony2plugin.tests.config.component.parser;

import com.intellij.openapi.vfs.VfsUtil;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

public class ParameterServiceParserTest extends SymfonyTempCodeInsightFixtureTestCase {

    public void testParse() throws Exception {
        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/component/parser/appDevDebugProjectContainer.xml");
        ParameterServiceParser parameterServiceParser = new ParameterServiceParser();
        parameterServiceParser.parser(new FileInputStream(testFile), VfsUtil.findFileByIoFile(testFile, true), getProject());

        Map<String, String> map = parameterServiceParser.getParameterMap();

        assertEquals("app", map.get("kernel.name"));
        assertEquals("Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerNameParser", map.get("controller_name_converter.class"));

        assertEquals("collection", map.get("kernel.bundles"));
        assertNull("collection", map.get("%kernel.bundles%"));
        assertNull(map.get("FrameworkBundle"));
    }

}
