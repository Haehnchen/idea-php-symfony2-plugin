package fr.adrienbrault.idea.symfony2plugin.tests.codeInsight.caret.overlay.provider;

import com.intellij.ide.highlighter.XmlFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.provider.XmlServiceContainerCaretTextOverlay
 */
public class XmlServiceContainerCaretTextOverlayTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.xml"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testConstructorParameterArguments() {

        String[] strings = {
            "<service id=\"F<caret>oo\" class=\"Foo\\Bar\"/>",
            "<serv<caret>ice id=\"Foo\" class=\"Foo\\Bar\"/>",
            "<serv<caret>ice id=\"Foo\"<caret> class=\"Foo\\Bar\"/>",
            "<serv<caret>ice id=\"Foo\" class=\"Foo\\<caret>Bar\"/>",
        };

        for (String s : strings) {
            assertCaretTextOverlay(XmlFileType.INSTANCE,
                "<container><services>" + s + "</services></container>",
                new CaretTextOverlay.TextEqualsAssert("(dateTime : \\DateTime, items : array)")
            );
        }
    }

    public void testParameterNaming() {
        String[] strings = {
            "<service><argument>%foo_parame<caret>ter_class%</argument></service>",
        };

        for (String s : strings) {
            assertCaretTextOverlay(XmlFileType.INSTANCE,
                "<container><services>" + s + "</services></container>",
                new CaretTextOverlay.TextEqualsAssert("Foo\\Bar")
            );
        }
    }

    public void testArgumentElementIsNotAParameter() {
        assertCaretTextOverlayEmpty(XmlFileType.INSTANCE,
            "<container><services>foo_parame<caret>ter_class</services></container>"
        );
    }

    public void testServiceInstanceNaming() {
        String[] strings = {
            "<service><argument type=\"service\" id=\"f<caret>oo\"/></service>",
            "<service><argument type=\"service\" id=\"f<caret>oo_bar\"/></service>",
        };

        for (String s : strings) {
            assertCaretTextOverlay(XmlFileType.INSTANCE,
                "<container><services>" + s + "</services></container>",
                new CaretTextOverlay.TextEqualsAssert("Foo\\Bar")
            );
        }
    }
}
