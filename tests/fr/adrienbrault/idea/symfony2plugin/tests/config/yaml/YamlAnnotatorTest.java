package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlAnnotator
 */
public class YamlAnnotatorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("YamlAnnotator.php"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("YamlAnnotator.xml"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testAnnotatorConstructorArguments() {
        String[] strings = {
            "@ar<caret>gs_bar",
            "\"@ar<caret>gs_bar\"",
            "'@ar<caret>gs_bar'",
        };

        for (String s : strings) {
            assertAnnotationContains("services.yml",
                "services:\n" +
                    "  foo:\n" +
                    "    class: \\Args\\Foo\n" +
                    "    arguments: [" + s + "]",
                "Expect instance of: Args\\Foo"
            );

            assertAnnotationContains("services.yml",
                "services:\n" +
                    "  foo:\n" +
                    "    class: \\Args\\Foo\n" +
                    "    arguments: [ @foo, %foo%, " + s + "]",
                "Expect instance of: Args\\Foo"
            );
        }
    }

    public void testAnnotatorConstructorArgumentsForServiceIdShortcut() {
        assertAnnotationContains("services.yml",
            "services:\n" +
                "  Args\\Foo:\n" +
                "    arguments: [ @foo, %foo%, '@ar<caret>gs_bar']",
            "Expect instance of: Args\\Foo"
        );

        assertAnnotationContains("services.yml",
            "services:\n" +
                "  foo:\n" +
                "    class: \\Args\\Foo\n" +
                "    calls:\n" +
                "     - [ setFoo, ['@ar<caret>gs_bar'] ]\n",
            "Expect instance of: Args\\Foo"
        );
    }

    public void testAnnotatorConstructorArgumentsAsSequence() {

        String[] strings = {
            "@ar<caret>gs_bar",
            "\"@ar<caret>gs_bar\"",
            "'@ar<caret>gs_bar'",
        };

        for (String s : strings) {
            assertAnnotationContains("services.yml",
                "services:\n" +
                    "  foo:\n" +
                    "    class: \\Args\\Foo\n" +
                    "    arguments:\n" +
                    "     - " + s + "\n",
                "Expect instance of: Args\\Foo"
            );

            assertAnnotationContains("services.yml",
                "services:\n" +
                    "  foo:\n" +
                    "    class: \\Args\\Foo\n" +
                    "    arguments:\n" +
                    "     - @foo\n" +
                    "     - %foo%\n" +
                    "     - " + s + "\n",
                "Expect instance of: Args\\Foo"
            );
        }
    }

    public void testAnnotatorCallsArguments() {
        String[] strings = {
            "@ar<caret>gs_bar",
            "\"@ar<caret>gs_bar\"",
            "'@ar<caret>gs_bar'",
        };

        for (String s : strings) {
            assertAnnotationContains("services.yml",
                "services:\n" +
                    "  foo:\n" +
                    "    class: \\Args\\Foo\n" +
                    "    calls:\n" +
                    "     - [ setFoo, [" + s + "] ]\n",
                "Expect instance of: Args\\Foo"
            );

            assertAnnotationContains("services.yml",
                "services:\n" +
                    "  foo:\n" +
                    "    class: \\Args\\Foo\n" +
                    "    calls:\n" +
                    "     - [ setFoo, [@foo, %foo%, " + s + "] ]\n",
                "Expect instance of: Args\\Foo"
            );
        }
    }
}
