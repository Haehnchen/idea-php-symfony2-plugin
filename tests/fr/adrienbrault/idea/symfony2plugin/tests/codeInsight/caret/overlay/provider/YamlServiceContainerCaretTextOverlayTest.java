package fr.adrienbrault.idea.symfony2plugin.tests.codeInsight.caret.overlay.provider;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.provider.YamlServiceContainerCaretTextOverlay
 */
public class YamlServiceContainerCaretTextOverlayTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.xml"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testParameterNaming() {

        for (String s : new String[] {
            "%foo_parame<caret>ter_class%",
            "%foo_parame<caret>ter_CLASS%",
        }) {
            assertCaretTextOverlay(YAMLFileType.YML, s, new CaretTextOverlay.TextEqualsAssert("Foo\\Bar"));
        }
    }

    public void testServiceInstanceNaming() {

        for (String s : new String[] {
            "@f<caret>oo",
            "@f<caret>OO",
            "foo: @f<caret>oo",
            "foo: '@f<caret>oo'",
            "foo: \"@f<caret>oo\"",
        }) {
            assertCaretTextOverlay(YAMLFileType.YML, s, new CaretTextOverlay.TextEqualsAssert("Foo\\Bar"));
        }
    }

    public void testConstructorParameterArguments() {
        for (String s : new String[] {
            "fo<caret>o: \n   class: Foo\\Bar",
            "fo<caret>o: \n   class: %foo_parameter_class%",
            "fo<caret>o: \n   class: %foo_parameter_CLASS%",
            "\"fo<caret>o\": \n   class: Foo\\Bar",
        }) {
            assertCaretTextOverlay(YAMLFileType.YML, s, new CaretTextOverlay.TextEqualsAssert("(dateTime : \\DateTime, items : array)"));
        }
    }
}
