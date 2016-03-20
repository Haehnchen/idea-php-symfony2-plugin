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

    public void testAnnotatorParameter() {
        assertAnnotationContains("services.yml", "services:\n   %foo_<caret>missing%", "Missing Parameter");
        assertAnnotationNotContains("services.yml", "services:\n   %foo_p<caret>arameter%", "Missing Parameter");

        assertAnnotationContains("services.yml", "services:\n   %Foo_<caret>missing%", "Missing Parameter");
        assertAnnotationNotContains("services.yml", "services:\n   %Foo_p<caret>arameter%", "Missing Parameter");

        assertAnnotationContains("services.yml", "services:\n   [ '%Foo_<caret>missing%' ]\n", "Missing Parameter");
        assertAnnotationContains("services.yml", "services:\n   [ \"%Foo_<caret>missing%\" ]\n", "Missing Parameter");
        assertAnnotationNotContains("services.yml", "services:\n   %kernel.root_dir%/../we<caret>b/%webpath_modelmasks%", "Missing Parameter");
    }

    public void testAnnotatorServiceName() {
        assertAnnotationContains("services.yml", "services:\n   @args<caret>_unknown", "Missing Service");
        assertAnnotationContains("services.yml", "services:\n   @Args<caret>_unknown", "Missing Service");
        assertAnnotationNotContains("services.yml", "services:\n   @args<caret>_foo", "Missing Service");
        assertAnnotationNotContains("services.yml", "services:\n   @Args<caret>_foo", "Missing Service");

        assertAnnotationNotContains("services.yml", "services:\n   @@args<caret>_unknown", "Missing Service");
        assertAnnotationNotContains("services.yml", "services:\n   @=args<caret>_unknown", "Missing Service");
    }

    public void testAnnotatorClass() {
        assertAnnotationContains("services.yml", "services:\n  class: Args\\Fo<caret>oBar", "Missing Class");
        assertAnnotationContains("services.yml", "services:\n  factory_class: Args\\Fo<caret>oBar", "Missing Class");
        assertAnnotationNotContains("services.yml", "services:\n  factory_class: Args\\Fo<caret>o", "Missing Class");

        assertAnnotationContains("services.yml", "parameters:\n  foo.class: Args\\Fo<caret>oBar", "Missing Class");
        assertAnnotationNotContains("services.yml", "parameters:\n  foo.class: Args\\Fo<caret>o", "Missing Class");
    }
}
