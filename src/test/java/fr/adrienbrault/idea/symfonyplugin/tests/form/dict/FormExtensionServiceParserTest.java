package fr.adrienbrault.idea.symfonyplugin.tests.form.dict;

import fr.adrienbrault.idea.symfonyplugin.form.dict.FormExtensionServiceParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormExtensionServiceParserTest extends Assert {

    @Test
    public void testParse() throws Exception {

        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/form/dict/appDevDebugProjectContainer.xml");

        FormExtensionServiceParser formExtensionServiceParser = new FormExtensionServiceParser();
        formExtensionServiceParser.parser(new FileInputStream(testFile));
        Map<String, String> parser = formExtensionServiceParser.getFormExtensions();

        assertEquals("form", parser.get("Symfony\\Component\\Form\\Extension\\Validator\\Type\\FormTypeValidatorExtension"));
        assertEquals("repeated", parser.get("Symfony\\Component\\Form\\Extension\\Validator\\Type\\RepeatedTypeValidatorExtension"));

    }

}
