package fr.adrienbrault.idea.symfony2plugin.tests.form.dict;

import com.intellij.openapi.vfs.VfsUtil;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormExtensionServiceParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormExtensionServiceParserTest extends SymfonyTempCodeInsightFixtureTestCase {

    public void testParse() throws Exception {
        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/form/dict/appDevDebugProjectContainer.xml");

        FormExtensionServiceParser formExtensionServiceParser = new FormExtensionServiceParser();
        formExtensionServiceParser.parser(new FileInputStream(testFile), VfsUtil.findFileByIoFile(testFile, true), getProject());
        Map<String, String> parser = formExtensionServiceParser.getFormExtensions();

        assertEquals("form", parser.get("Symfony\\Component\\Form\\Extension\\Validator\\Type\\FormTypeValidatorExtension"));
        assertEquals("repeated", parser.get("Symfony\\Component\\Form\\Extension\\Validator\\Type\\RepeatedTypeValidatorExtension"));
    }

}
