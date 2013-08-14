package fr.adrienbrault.idea.symfony2plugin.tests.form.dict;

import fr.adrienbrault.idea.symfony2plugin.form.dict.FormExtensionServiceParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormExtensionServiceParserTest extends Assert {

    @Test
    public void testParse() throws Exception {

        File testFile = new File(this.getClass().getResource("appDevDebugProjectContainer.xml").getFile());

        FormExtensionServiceParser formExtensionServiceParser = new FormExtensionServiceParser();
        formExtensionServiceParser.parser(testFile);
        HashMap<String, String> parser = formExtensionServiceParser.getFormExtensions();

        assertEquals("form", parser.get("Symfony\\Component\\Form\\Extension\\Validator\\Type\\FormTypeValidatorExtension"));
        assertEquals("repeated", parser.get("Symfony\\Component\\Form\\Extension\\Validator\\Type\\RepeatedTypeValidatorExtension"));

    }

}
