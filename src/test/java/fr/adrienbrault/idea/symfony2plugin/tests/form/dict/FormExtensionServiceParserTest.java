package fr.adrienbrault.idea.symfony2plugin.tests.form.dict;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormExtensionServiceParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.InputStream;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormExtensionServiceParserTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/form/dict";
    }

    public void testParse() throws Exception {
        VirtualFile testFile = myFixture.copyFileToProject("appDevDebugProjectContainer.xml");

        FormExtensionServiceParser formExtensionServiceParser = new FormExtensionServiceParser();
        try (InputStream inputStream = testFile.getInputStream()) {
            formExtensionServiceParser.parser(inputStream, testFile, getProject());
        }
        Map<String, String> parser = formExtensionServiceParser.getFormExtensions();

        assertEquals("form", parser.get("Symfony\\Component\\Form\\Extension\\Validator\\Type\\FormTypeValidatorExtension"));
        assertEquals("repeated", parser.get("Symfony\\Component\\Form\\Extension\\Validator\\Type\\RepeatedTypeValidatorExtension"));
    }

}
