package fr.adrienbrault.idea.symfonyplugin.tests.dic.linemarker;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.dic.linemarker.XmlLineMarkerProvider
 */
public class XmlLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.xml"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/dic/linemarker/fixtures";
    }

    public void testThatDecoratedServiceShouldProvideMarker() {
        assertLineMarker(createXmlFile("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "       <service id=\"foo_bar_main\" class=\"Foo\\Bar\\Apple\"/>\n" +
                "    </services>\n" +
                "</container>"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to decoration")
        );
    }

    public void testThatParentServiceShouldProvideMarker() {
        assertLineMarker(createXmlFile("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "       <service id=\"foo_bar_main\" class=\"Foo\\Bar\\Apple\"/>\n" +
                "    </services>\n" +
                "</container>"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to decoration")
        );
    }

    public void testThatDecoratesProvidesOverwriteMarker() {
        assertLineMarker(createXmlFile("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "       <service id=\"foo_bar_main\" decorates=\"app.mailer\"/>\n" +
                "    </services>\n" +
                "</container>"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to decorated service")
        );
    }

    public void testThatParentProvidesOverwriteMarker() {
        assertLineMarker(createXmlFile("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "       <service id=\"foo_bar_main\" parent=\"app.mailer\"/>\n" +
                "    </services>\n" +
                "</container>"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to parent service")
        );
    }

    @NotNull
    private PsiElement createXmlFile(@NotNull String content) {
        return PsiFileFactory.getInstance(getProject()).createFileFromText("DUMMY__." + XmlFileType.INSTANCE.getDefaultExtension(), XmlFileType.INSTANCE, content);
    }
}
