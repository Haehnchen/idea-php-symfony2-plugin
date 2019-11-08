package fr.adrienbrault.idea.symfonyplugin.tests.config.component.parser;

import fr.adrienbrault.idea.symfonyplugin.config.component.parser.ParameterServiceParser;
import org.junit.Assert;
import org.junit.Test;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

public class ParameterServiceParserTest extends Assert {

    @Test
    public void testParse() throws Exception {

        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/config/component/parser/appDevDebugProjectContainer.xml");
        ParameterServiceParser parameterServiceParser = new ParameterServiceParser();
        parameterServiceParser.parser(new FileInputStream(testFile));

        Map<String, String> map = parameterServiceParser.getParameterMap();

        assertEquals("app", map.get("kernel.name"));
        assertEquals("Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerNameParser", map.get("controller_name_converter.class"));

        assertEquals("collection", map.get("kernel.bundles"));
        assertNull("collection", map.get("%kernel.bundles%"));
        assertNull(map.get("FrameworkBundle"));
    }

}
