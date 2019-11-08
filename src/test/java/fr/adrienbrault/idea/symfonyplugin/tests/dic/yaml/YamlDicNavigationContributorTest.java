package fr.adrienbrault.idea.symfonyplugin.tests.dic.yaml;

import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.config.yaml.YamlCompletionContributor
 */
public class YamlDicNavigationContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

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
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/dic";
    }

    public void testFactoryClassMethodNavigation() {

        assertNavigationContains(YAMLFileType.YML, "services:\n" +
                "    foo.factory:\n" +
                "        class: Foo\\Name\\FooClass\n" +
                "    foo.manager:\n" +
                "        factory: [\"@foo.factory\", <caret>foo ]\n"
            , "Foo\\Name\\FooClass::foo"
        );

        assertNavigationContains(YAMLFileType.YML, "services:\n" +
                "    foo.factory:\n" +
                "        class: Foo\\Name\\FooClass\n" +
                "    foo.manager:\n" +
                "        factory: [@foo.factory, <caret>foo ]\n"
            , "Foo\\Name\\FooClass::foo"
        );

        assertNavigationContains(YAMLFileType.YML, "services:\n" +
                "    foo.factory:\n" +
                "        class: Foo\\Name\\FooClass\n" +
                "    foo.manager:\n" +
                "        factory: ['@foo.factory', <caret>foo ]\n"
            , "Foo\\Name\\FooClass::foo"
        );
    }

    public void testFactoryClassMethodnavigationForStringShortcut() {
        assertNavigationContains(YAMLFileType.YML, "services:\n" +
                "    foo.factory:\n" +
                "        class: Foo\\Name\\FooClass\n" +
                "    foo.manager:\n" +
                "        factory: 'foo.factory:f<caret>oo'\n"
            , "Foo\\Name\\FooClass::foo"
        );
    }
}
