package fr.adrienbrault.idea.symfony2plugin.tests.codeInsight.caret.overlay.provider;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.provider.YamlServiceContainerCaretTextOverlay;
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

    /**
     * @see YamlServiceContainerCaretTextOverlay#getOverlay
     */
    public void testParameterNaming() {

        for (String s : new String[] {
            "%foo_parame<caret>ter_class%",
            "%foo_parame<caret>ter_CLASS%",
        }) {
            assertCaretTextOverlay(YAMLFileType.YML, s, new CaretTextOverlay.TextEqualsAssert("Foo\\Bar"));
        }
    }

    /**
     * @see YamlServiceContainerCaretTextOverlay#getOverlay
     */
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

    /**
     * @see YamlServiceContainerCaretTextOverlay#getOverlay
     */
    public void testConstructorParameterArguments() {
        for (String s : new String[] {
            "services:\n  fo<caret>o: \n   class: Foo\\Bar",
            "services:\n fo<caret>o: \n   class: %foo_parameter_class%",
            "services:\n fo<caret>o: \n   class: %foo_parameter_CLASS%",
            "services:\n \"fo<caret>o\": \n   class: Foo\\Bar",
        }) {
            assertCaretTextOverlay(YAMLFileType.YML, s, new CaretTextOverlay.TextEqualsAssert("(dateTime : \\DateTime, items : array)"));
        }

        assertCaretTextOverlayEmpty(YAMLFileType.YML, "fo<caret>o: \n   class: Foo\\Bar");
    }

    /**
     * @see YamlServiceContainerCaretTextOverlay#getOverlay
     */
    public void testGlobalClassKeyValue() {
        for (String s : new String[] {
            "class: Foo<caret>\\Bar",
            "class: 'Foo<caret>\\Bar'",
            "class: \"Foo<caret>\\Bar\"",
            "class: %foo_paramet<caret>er_class%",
            "class: %foo_parame<caret>ter_CLASS%",
            "class: '%foo_parame<caret>ter_CLASS%'",
        }) {
            assertCaretTextOverlay(YAMLFileType.YML, s, new CaretTextOverlay.TextEqualsAssert("(dateTime : \\DateTime, items : array)"));
        }
    }

    /**
     * @see YamlServiceContainerCaretTextOverlay#getOverlay
     */
    public void testArgumentsOfServiceAreNotDisplayedOnNonConstructor() {
        assertCaretTextOverlayEmpty(YAMLFileType.YML,
            "services:\n" +
                "  bar_<caret>foo._bar:\n" +
                "    class: Iterator"
        );

        assertCaretTextOverlayEmpty(YAMLFileType.YML, "class: It<caret>erator");
    }
}
