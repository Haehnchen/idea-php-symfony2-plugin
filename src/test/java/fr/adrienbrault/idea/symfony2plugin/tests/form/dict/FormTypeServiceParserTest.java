package fr.adrienbrault.idea.symfony2plugin.tests.form.dict;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeServiceParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.InputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeServiceParserTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/form/dict";
    }

    public void testParse() throws Exception {
        VirtualFile testFile = myFixture.copyFileToProject("appDevDebugProjectContainer.xml");

        FormTypeServiceParser parser = new FormTypeServiceParser();
        try (InputStream inputStream = testFile.getInputStream()) {
            parser.parser(inputStream, testFile, getProject());
        }

        assertEquals("field", parser.getFormTypeMap().getMap().get("form.type.field"));
        assertEquals("locale", parser.getFormTypeMap().getMap().get("form.type.locale"));
        assertEquals("entity", parser.getFormTypeMap().getMap().get("form.type.entity"));

        assertEquals("form.type.entity", parser.getFormTypeMap().getServiceName("entity"));
    }

}
