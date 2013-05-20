package fr.adrienbrault.idea.symfony2plugin.tests.form.dict;

import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeMap;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeParser;

import org.junit.Assert;
import org.junit.Test;
import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypesParserTest extends Assert {

    @Test
    public void testParse() throws Exception {

        File testFile = new File(this.getClass().getResource("appDevDebugProjectContainer.xml").getFile());

        FormTypeParser parser = new FormTypeParser();
        FormTypeMap map = parser.parse(testFile);

        assertEquals(map.getMap().get("form.type.field"), "field");
        assertEquals(map.getMap().get("form.type.locale"), "locale");
        assertEquals(map.getMap().get("form.type.entity"), "entity");

    }

}
