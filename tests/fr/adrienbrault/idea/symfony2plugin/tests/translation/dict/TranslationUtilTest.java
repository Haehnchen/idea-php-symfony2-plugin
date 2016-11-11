package fr.adrienbrault.idea.symfony2plugin.tests.translation.dict;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xml.XmlFileTypeFactory;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;

import java.io.File;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil
 */
public class TranslationUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("apple.de.yml", "Resources/translations/apple.de.yml");
        myFixture.copyFileToProject("car.de.yml", "Resources/translations/car.de.yml");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testGetDomainFilePsiElements() {
        assertTrue(TranslationUtil.getDomainPsiFiles(getProject(), "apple").size() > 0);
        assertTrue(TranslationUtil.getDomainPsiFiles(getProject(), "car").size() > 0);
    }

    public void testGetTranslationPsiElements() {
        assertTrue(TranslationUtil.getTranslationPsiElements(getProject(), "yaml_weak.symfony.great", "apple").length > 0);
        assertTrue(TranslationUtil.getTranslationPsiElements(getProject(), "yaml_weak.symfony.greater than", "apple").length > 0);
        assertTrue(TranslationUtil.getTranslationPsiElements(getProject(), "yaml_weak.symfony.greater than equals", "apple").length > 0);

        assertTrue(TranslationUtil.getTranslationPsiElements(getProject(), "yaml_weak.symfony.more.lines", "apple").length > 0);
        assertTrue(TranslationUtil.getTranslationPsiElements(getProject(), "yaml_weak.symfony.more.lines_2", "apple").length > 0);

        assertTrue(TranslationUtil.getTranslationPsiElements(getProject(), "foo_yaml.symfony.great", "car").length > 0);
    }

    public void testGetTargetForXlfAsXmlFileInVersion12() {
        PsiFile fileFromText = PsiFileFactory.getInstance(getProject()).createFileFromText(XMLLanguage.INSTANCE, "" +
            "<?xml version=\"1.0\"?>\n" +
            "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\">\n" +
            "    <file source-language=\"en\" datatype=\"plaintext\" original=\"file.ext\">\n" +
            "        <body>\n" +
            "            <trans-unit id=\"1\">\n" +
            "                <source>This value should be false.</source>\n" +
            "            </trans-unit>\n" +
            "        </body>\n" +
            "    </file>\n" +
            "</xliff>\n"
        );

        Collection<PsiElement> files = TranslationUtil.getTargetForXlfAsXmlFile((XmlFile) fileFromText, "This value should be false.");

        assertNotNull(ContainerUtil.find(files, psiElement ->
            psiElement instanceof XmlTag && "This value should be false.".equals(((XmlTag) psiElement).getValue().getText()))
        );
    }

    public void testGetTargetForXlfAsXmlFileInVersion20() {
        PsiFile fileFromText = PsiFileFactory.getInstance(getProject()).createFileFromText(XMLLanguage.INSTANCE, "" +
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
            "        </group>\n" +
            "    </file>\n" +
            "</xliff>"
        );

        Collection<PsiElement> files = TranslationUtil.getTargetForXlfAsXmlFile((XmlFile) fileFromText, "foo");

        assertNotNull(ContainerUtil.find(files, psiElement ->
            psiElement instanceof XmlTag && "foo".equals(((XmlTag) psiElement).getValue().getText()))
        );
    }
}
