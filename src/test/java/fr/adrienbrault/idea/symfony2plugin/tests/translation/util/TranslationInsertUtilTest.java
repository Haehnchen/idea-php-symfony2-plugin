package fr.adrienbrault.idea.symfony2plugin.tests.translation.util;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.xml.XmlFile;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.translation.util.TranslationInsertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlPsiElementFactory;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationInsertUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testInsertTranslationForXlf() {
        PsiFile psiFile = PsiFileFactory.getInstance(getProject()).createFileFromText("foo.xml", XmlFileType.INSTANCE, "" +
            "<?xml version=\"1.0\"?>\n" +
            "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\">\n" +
            "    <file source-language=\"en\" datatype=\"plaintext\" original=\"file.ext\">\n" +
            "        <body>\n" +
            "            <trans-unit id=\"1\">\n" +
            "                <source>This value should be false.</source>\n" +
            "            </trans-unit>\n" +
            "            <trans-unit id=\"foobar\">\n" +
            "                <source>This value should be false.</source>\n" +
            "            </trans-unit>\n" +
            "        </body>\n" +
            "    </file>\n" +
            "</xliff>\n"
        );

        CommandProcessor.getInstance().executeCommand(getProject(), () -> ApplicationManager.getApplication().runWriteAction(() -> {
            TranslationInsertUtil.invokeTranslation((XmlFile) psiFile, "foobar", "value");
        }), null, null);

        String text = psiFile.getText();

        assertTrue(text.contains("<trans-unit id=\"2\">"));
        assertTrue(text.contains("<source>foobar</source>"));
        assertTrue(text.contains("<target>value</target>"));
    }

    public void testInsertTranslationForXlf20() {
        PsiFile xmlFile = PsiFileFactory.getInstance(getProject()).createFileFromText("foo.xml", XmlFileType.INSTANCE, "" +
            "<?xml version=\"1.0\"?>\n" +
            "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\"\n" +
            "       version=\"2.0\" srcLang=\"en-US\" trgLang=\"ja-JP\">\n" +
            "    <file id=\"f1\" original=\"Graphic Example.psd\">\n" +
            "        <skeleton href=\"Graphic Example.psd.skl\"/>\n" +
            "        <group id=\"1\">\n" +
            "            <unit id=\"1\">\n" +
            "                <segment>\n" +
            "                    <source>foo</source>\n" +
            "                </segment>\n" +
            "            </unit>\n" +
            "            <unit id=\"foobar\">\n" +
            "                <segment>\n" +
            "                    <source>foo</source>\n" +
            "                </segment>\n" +
            "            </unit>\n" +
            "        </group>\n" +
            "    </file>\n" +
            "</xliff>"
        );

        CommandProcessor.getInstance().executeCommand(getProject(), () -> ApplicationManager.getApplication().runWriteAction(() -> {
            TranslationInsertUtil.invokeTranslation((XmlFile) xmlFile, "foobar", "value");
        }), null, null);

        String text = xmlFile.getText();

        assertTrue(text.contains("<unit id=\"2\">"));
        assertTrue(text.contains("<source>foobar</source>"));
        assertTrue(text.contains("<target>value</target>"));
    }

    public void testInsertTranslationForXlf20Shortcut() {
        PsiFile xmlFile = PsiFileFactory.getInstance(getProject()).createFileFromText("foo.xml", XmlFileType.INSTANCE, "" +
            "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\"\n" +
            " srcLang=\"en-US\" trgLang=\"ja-JP\">\n" +
            " <file id=\"f1\" original=\"Graphic Example.psd\">\n" +
            "  <skeleton href=\"Graphic Example.psd.skl\"/>\n" +
            "  <unit id=\"1\">\n" +
            "   <segment>\n" +
            "    <source>foo</source>\n" +
            "   </segment>\n" +
            "  </unit>\n" +
            "  <unit id=\"foobar\">\n" +
            "   <segment>\n" +
            "    <source>foo</source>\n" +
            "   </segment>\n" +
            "  </unit>\n" +
            " </file>\n" +
            "</xliff>"
        );

        CommandProcessor.getInstance().executeCommand(getProject(), () -> ApplicationManager.getApplication().runWriteAction(() -> {
            TranslationInsertUtil.invokeTranslation((XmlFile) xmlFile, "foobar", "value");
        }), null, null);

        String text = xmlFile.getText();

        assertTrue(text.contains("<unit id=\"2\">"));
        assertTrue(text.contains("<source>foobar</source>"));
        assertTrue(text.contains("<target>value</target>"));
    }

    public void testInsertTranslationForYamlFile() {
        PsiFile dummyFile = YamlPsiElementFactory.createDummyFile(getProject(), "foo.de.yml", "car: 'foo'");

        CommandProcessor.getInstance().executeCommand(getProject(), () -> ApplicationManager.getApplication().runWriteAction(() -> {
            TranslationInsertUtil.invokeTranslation(dummyFile, "foobar", "value");
        }), null, null);

        String text = dummyFile.getText();

        assertTrue(text.contains("foobar: 'value'"));
    }
}
