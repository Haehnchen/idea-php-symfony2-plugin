package fr.adrienbrault.idea.symfony2plugin.tests.config.component.parser;

import org.junit.Assert;
import org.junit.Test;
import java.io.File;
import java.util.Map;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterParser;

public class ParameterParserTest extends Assert {

    @Test
    public void testParse() throws Exception {

        File testFile = new File(this.getClass().getResource("appDevDebugProjectContainer.xml").getFile());
        Map map = new ParameterParser().parse(testFile);

        assertEquals("app", map.get("%kernel.name%"));
        assertEquals("Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerNameParser", map.get("%controller_name_converter.class%"));

        assertEquals("collection", map.get("%kernel.bundles%"));
        assertNull("collection", map.get("kernel.bundles"));
        assertNull(map.get("%FrameworkBundle%"));
    }

}
