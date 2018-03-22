package fr.adrienbrault.idea.symfony2plugin.tests.dic.yaml;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlCompletionContributor
 */
public class YamlDicCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("appDevDebugProjectContainer.xml");

        myFixture.configureByText("classes.php", "<?php\n" +
            "namespace Foo\\Name;\n" +
            "class FooClass {" +
            " public function foo() " +
            "}"
        );

    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("..").getFile()).getAbsolutePath();
    }

    public void testServiceCompletion() {

        assertCompletionContains(YAMLFileType.YML, "@<caret>", "data_collector.router");

        assertCompletionContains(YAMLFileType.YML, "services:\n" +
                "    foo:\n" +
                "        arguments: [\"@<caret>\"] "
            , "data_collector.router"
        );

        assertCompletionContains(YAMLFileType.YML, "services:\n" +
                "    foo:\n" +
                "        arguments: ['@<caret>'] "
            , "data_collector.router"
        );

        assertCompletionContains(YAMLFileType.YML, "services:\n" +
            "    foo:\n" +
            "        arguments: [@<caret>] "
        , "data_collector.router"
        );

        assertCompletionContains(YAMLFileType.YML, "services:\n" +
            "    my_service:\n" +
            "        factory_service: <caret>\n"
            , "data_collector.router"
        );

        assertCompletionContains(YAMLFileType.YML, "services:\n" +
            "    newsletter_manager:\n" +
            "        parent: @<caret>\n"
            , "data_collector.router"
        );

        assertCompletionContains(YAMLFileType.YML, "services:\n" +
                "    newsletter_manager:\n" +
                "        parent: @<caret>\n"
            , "data_collector.router"
        );

    }

    public void testServiceStaticCompletion() {

        assertCompletionContains(YAMLFileType.YML, "services:\n" +
                "    newsletter_manager:\n" +
                "        @<caret>\n"
            , "arguments", "calls"
        );

    }

    public void testClassesCompletion() {

        assertCompletionContains(YAMLFileType.YML, "services:\n" +
                "    espend_container_service.yaml:\n" +
                "        class: <caret>\n"
            , "FooClass"
        );

        /* todo: contains check; char switch expected "(" "["
        assertCompletionLookupTailEquals(YAMLFileType.YML, "services:\n" +
                "    espend_container_service.yaml:\n" +
                "        class: <caret>\n"
            , "FooClass", " (Foo\\Name)"
        );
        */

        assertCompletionContains(YAMLFileType.YML, "services:\n" +
                "    newsletter_manager:\n" +
                "        factory_class: <caret>\n"
            , "FooClass"
        );

    }

    public void testClassCompletionResult() {

        assertCompletionResultEquals("service.yml",
            "services:\n" +
                "    espend_container_service.yaml:\n" +
                "        class: FooClass<caret>\n",
            "services:\n" +
                "    espend_container_service.yaml:\n" +
                "        class: Foo\\Name\\FooClass\n"
        );

        assertCompletionResultEquals("service.yml",
            "services:\n" +
                "    espend_container_service.yaml:\n" +
                "        class: Foo\\Name\\<caret>\n",
            "services:\n" +
                "    espend_container_service.yaml:\n" +
                "        class: Foo\\Name\\FooClass\n"
        );

    }

    public void testFactoryClassMethodCompletionResult() {

        assertCompletionContains(YAMLFileType.YML, "services:\n" +
                "    foo.factory:\n" +
                "        class: Foo\\Name\\FooClass\n" +
                "    foo.manager:\n" +
                "        factory: [\"@foo.factory\", <caret>]\n"
            , "foo"
        );

        assertCompletionContains(YAMLFileType.YML, "services:\n" +
                "    foo.factory:\n" +
                "        class: Foo\\Name\\FooClass\n" +
                "    foo.manager:\n" +
                "        factory: [@foo.factory, <caret>]\n"
            , "foo"
        );

        assertCompletionContains(YAMLFileType.YML, "services:\n" +
                "    foo.factory:\n" +
                "        class: Foo\\Name\\FooClass\n" +
                "    foo.manager:\n" +
                "        factory: ['@foo.factory', <caret>]\n"
            , "foo"
        );

    }

    public void testFactoryClassMethodCompletionForStringShortcut() {
        assertCompletionContains(YAMLFileType.YML, "services:\n" +
                "    foo.manager:\n" +
                "        factory: '<caret>'\n"
            , "data_collector.router"
        );
    }
}
