package fr.adrienbrault.idea.symfony2plugin.tests.dic.linemarker;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.linemarker.XmlLineMarkerProvider
 */
public class XmlLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.xml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("ContainerBuilder.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
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

    public void testThatContainerBuilderProvidesRelatedServiceNavigation() {
        assertLineMarker(createXmlFile("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "       <service id=\"foo_bar_main\"/>\n" +
                "    </services>\n" +
                "</container>"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to definition call")
        );
    }

    @NotNull
    private PsiElement createXmlFile(@NotNull String content) {
        return PsiFileFactory.getInstance(getProject()).createFileFromText("DUMMY__." + XmlFileType.INSTANCE.getDefaultExtension(), XmlFileType.INSTANCE, content);
    }
}
