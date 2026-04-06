package fr.adrienbrault.idea.symfony2plugin.tests.config.component.parser;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.InputStream;
import java.util.Map;

public class ParameterServiceParserTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/component/parser";
    }

    public void testParse() throws Exception {
        VirtualFile testFile = myFixture.copyFileToProject("appDevDebugProjectContainer.xml");
        ParameterServiceParser parameterServiceParser = new ParameterServiceParser();
        try (InputStream inputStream = testFile.getInputStream()) {
            parameterServiceParser.parser(inputStream, testFile, getProject());
        }

        Map<String, String> map = parameterServiceParser.getParameterMap();

        assertEquals("app", map.get("kernel.name"));
        assertEquals("Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerNameParser", map.get("controller_name_converter.class"));

        assertEquals("collection", map.get("kernel.bundles"));
        assertNull("collection", map.get("%kernel.bundles%"));
        assertNull(map.get("FrameworkBundle"));
    }

}
