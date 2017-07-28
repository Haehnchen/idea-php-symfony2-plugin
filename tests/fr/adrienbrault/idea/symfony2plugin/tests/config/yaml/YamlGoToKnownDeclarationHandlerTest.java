package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlGoToKnownDeclarationHandler
 */
public class YamlGoToKnownDeclarationHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("YamlGoToKnownDeclarationHandlerConfig.php");
        myFixture.copyFileToProject("classes.php");
        myFixture.configureByText("config_foo.yml", "");
        myFixture.configureByFile("tagged.services.xml");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testResourcesInsideSameDirectoryProvidesNavigation() {
        assertNavigationContainsFile(YAMLFileType.YML, "imports:\n" +
                "    - { resource: config_<caret>foo.yml }",
            "config_foo.yml"
        );

        assertNavigationContainsFile(YAMLFileType.YML, "imports:\n" +
                "    - { resource: 'config_<caret>foo.yml' }",
            "config_foo.yml"
        );

        assertNavigationContainsFile(YAMLFileType.YML, "imports:\n" +
                "    - { resource: \"config_<caret>foo.yml\" }",
            "config_foo.yml"
        );
    }

    public void testConfigKeyToTreeConfigurationNavigation() {
        assertNavigationMatch("config.yml", "foobar<caret>_root:\n" +
                "    foo: ~",
            PlatformPatterns.psiElement(StringLiteralExpression.class)
        );
    }

    public void testNavigateToTaggedServices() {
        String[] values = {"my_nice<caret>_tag", "'my_nice<caret>_tag'", "\"my_nice<caret>_tag\""};

        for (String value : values) {
            assertNavigationMatch("services.yml", "" +
                    "services:\n" +
                    "    foo:\n" +
                    "       tags: { name: " + value + " }\n",
                PlatformPatterns.psiElement(PhpClass.class)
            );
        }
    }

    public void testNavigateToTaggedServicesForSymfony33Shortcut() {
        String[] values = {"my_nice<caret>_tag", "'my_nice<caret>_tag'", "\"my_nice<caret>_tag\""};

        for (String value : values) {
            assertNavigationMatch("services.yml", "" +
                    "services:\n" +
                    "    foo:\n" +
                    "       tags: [ " + value +" ]\n",
                PlatformPatterns.psiElement(PhpClass.class)
            );
        }
    }

    public void testNavigateToClassServiceAsKeyForSymfony33() {
        assertNavigationMatch("services.yml", "" +
                "services:\n" +
                "    Fo<caret>o\\Bar: ~\n" +
            PlatformPatterns.psiElement(PhpClass.class)
        );
    }

    public void testNavigateForCallsMethodIsProvided() {
        assertNavigationMatch("services.yml", "" +
            "services:\n" +
            "    foobar:\n" +
            "       class: Foo\\Bar\n" +
            "       calls:\n" +
            "           - [ set<caret>Bar, [@foo]]\n" +
            PlatformPatterns.psiElement(PhpClass.class)
        );
    }

    public void testThatNavigationForControllerInvokeMethodIsAvailable() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "class Foobar\n" +
            "{\n" +
            "   public function __invoke() {}\n" +
            "}\n"
        );

        assertNavigationMatch("routing.yml", "" +
            "foobar:\n" +
            "    defaults:\n" +
            "       _controller: Foo<caret>bar\n",
            PlatformPatterns.psiElement(Method.class)
        );
    }
}
