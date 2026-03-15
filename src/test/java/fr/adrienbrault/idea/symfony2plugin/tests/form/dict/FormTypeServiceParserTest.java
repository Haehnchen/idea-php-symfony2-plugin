package fr.adrienbrault.idea.symfony2plugin.tests.form.dict;

import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeServiceParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

import com.intellij.openapi.vfs.VfsUtil;

import java.io.File;
import java.io.FileInputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeServiceParserTest extends SymfonyTempCodeInsightFixtureTestCase {

    public void testParse() throws Exception {
        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/form/dict/appDevDebugProjectContainer.xml");

        FormTypeServiceParser parser = new FormTypeServiceParser();
        parser.parser(new FileInputStream(testFile), VfsUtil.findFileByIoFile(testFile, true), getProject());

        assertEquals("field", parser.getFormTypeMap().getMap().get("form.type.field"));
        assertEquals("locale", parser.getFormTypeMap().getMap().get("form.type.locale"));
        assertEquals("entity", parser.getFormTypeMap().getMap().get("form.type.entity"));

        assertEquals("form.type.entity", parser.getFormTypeMap().getServiceName("entity"));
    }

}
