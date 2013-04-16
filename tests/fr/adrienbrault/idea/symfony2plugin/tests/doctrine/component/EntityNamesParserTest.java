package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.component;

import fr.adrienbrault.idea.symfony2plugin.doctrine.component.EntityNamesParser;
import org.junit.Assert;
import org.junit.Test;
import java.io.File;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EntityNamesParserTest extends Assert {

    @Test
    public void testParse() throws Exception {

        File testFile = new File(this.getClass().getResource("appDevDebugProjectContainer.xml").getFile());
        Map map = new EntityNamesParser().parse(testFile);

        assertEquals("\\My\\NiceBundle\\Entity", map.get("MyNiceBundle"));
        assertEquals("\\Your\\TestBundle\\Entity", map.get("YourTestBundle"));
    }

}

