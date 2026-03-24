package fr.adrienbrault.idea.symfony2plugin.tests.dic.yaml;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlCompletionContributor
 */
public class YamlDicNavigationContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("appDevDebugProjectContainer.xml");
        myFixture.copyFileToProject("container/util/fixtures/services_array.php", "services_array.php");

        myFixture.configureByText("classes.php", "<?php\n" +
            "namespace Foo\\Name;\n" +
            "class FooClass {" +
            " public function foo() " +
            "}"
        );

    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic";
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

    public void testPhpArrayServiceNavigation() {
        assertNavigationMatch(YAMLFileType.YML, "services:\n" +
                "    foo:\n" +
                "        arguments: ['@app.my<caret>_service']\n",
            PlatformPatterns.psiElement(StringLiteralExpression.class)
        );
    }
}
