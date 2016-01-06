package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml;

import fr.adrienbrault.idea.symfony2plugin.intentions.yaml.YamlServiceArgumentInspection;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.yaml.YamlServiceArgumentInspection
 */
public class YamlServiceArgumentInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testMissingArgumentProvidesInspection() {
        assertLocalInspectionContains("services.yml", "services:\n" +
                "  f<caret>oo:\n" +
                "    class: \\Foo\\Bar\n" +
                "    arguments: []",
            "Missing argument"
        );

        assertLocalInspectionContains("services.yml", "services:\n" +
                "  f<caret>oo:\n" +
                "    class: \\Foo\\Bar\n",
            "Missing argument"
        );
    }

    public void testThatAllParametersAreGivenWithLastOneOptional() {
        for (String s : new String[]{"@foo", "'@foo'", "\"@foo\""}) {
            assertLocalInspectionIsEmpty("services.yml", String.format("services:\n" +
                "  f<caret>oo:\n" +
                "    class: \\Foo\\Apple\n" +
                "    arguments: [%s]"
            , s));
        }
    }

    public void testThatNotSupportServiceAttributeNotProvidesInspection() {
        for (String invalidKey : YamlServiceArgumentInspection.INVALID_KEYS) {
            assertLocalInspectionIsEmpty("services.yml", String.format("services:\n" +
                "  f<caret>oo:\n" +
                "    class: \\Foo\\Bar\n" +
                "    %s: ~"
            , invalidKey));
        }
    }

}
