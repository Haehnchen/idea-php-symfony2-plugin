package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlGoToDeclarationHandler
 */
public class YamlGoToDeclarationHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("services.xml");
        myFixture.copyFileToProject("YamlGoToDeclarationHandler.php");
        myFixture.copyFileToProject("YamlGoToDeclarationHandler.env");

        myFixture.copyFileToProject("YamlGoToKnownDeclarationHandlerConfig.php");
        myFixture.copyFileToProject("classes.php");
        myFixture.configureByText("config_foo.yml", "");
        myFixture.configureByFile("tagged.services.xml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/yaml/fixtures";
    }

    public void testGlobalServiceName() {
        assertNavigationMatch(YAMLFileType.YML, "bar: f<caret>oo", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: [ f<caret>oo ]", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: { f<caret>oo }", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar:\n  - f<caret>oo", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: [ bar, f<caret>oo ]", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: { bar: f<caret>oo }", getClassPattern());

        assertNavigationIsEmpty(YAMLFileType.YML, "fo<caret>o:\n  - foo");
        assertNavigationIsEmpty(YAMLFileType.YML, "bar: { f<caret>oo: bar }");

        assertNavigationMatch(YAMLFileType.YML, "bar: foo.ba<caret>r_foo", getClassPattern());
        assertNavigationIsEmpty(YAMLFileType.YML, "bar: foo.<caret>bar-foo");
    }

    public void testGlobalServiceNameQuote() {
        assertNavigationMatch(YAMLFileType.YML, "bar: 'f<caret>oo'", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: [ 'f<caret>oo' ]", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: { 'f<caret>oo' }", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar:\n  - 'f<caret>oo'", getClassPattern());
        assertNavigationIsEmpty(YAMLFileType.YML, "'fo<caret>o':\n  - foo");

        assertNavigationMatch(YAMLFileType.YML, "bar: \"f<caret>oo\"", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: [ \"f<caret>oo\" ]", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: { \"f<caret>oo\" }", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar:\n  - \"f<caret>oo\"", getClassPattern());
        assertNavigationIsEmpty(YAMLFileType.YML, "\"fo<caret>o\":\n  - foo");

    }

    public void testSpecialCharPrefix() {
        assertNavigationMatch(YAMLFileType.YML, "bar: @f<caret>oo", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: @?f<caret>oo=", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: @?f<caret>oo", getClassPattern());
    }

    public void testSpecialCharPrefixQuote() {
        assertNavigationMatch(YAMLFileType.YML, "bar: '@f<caret>oo'", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: '@?f<caret>oo='", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: '@?f<caret>oo'", getClassPattern());

        assertNavigationMatch(YAMLFileType.YML, "bar: \"@f<caret>oo\"", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: \"@?f<caret>oo=\"", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: \"@?f<caret>oo\"", getClassPattern());
    }

    public void testPhpConstantNavigation() {
        assertNavigationMatch(YAMLFileType.YML, "bar: !php/const:\\YAML_<caret>FOO_BAR");
        assertNavigationMatch(YAMLFileType.YML, "bar: !php/const:YAML_<caret>FOO_BAR");
        assertNavigationMatch(YAMLFileType.YML, "bar: !php/const:Yaml\\Foo\\Bar::YAML_FOO_BAR<caret>_CLASS");
        assertNavigationMatch(YAMLFileType.YML, "bar: !php/const:Yaml\\Foo\\Bar::::YAML_FOO_BAR<caret>_CLASS");
        assertNavigationMatch(YAMLFileType.YML, "bar: !php/const:Yaml\\Foo\\Bar:YAML_FOO_BAR<caret>_CLASS");
        assertNavigationMatch(YAMLFileType.YML, "bar: !php/const:\\Yaml\\Foo\\Bar:YAML_FOO_BAR<caret>_CLASS");

        assertNavigationMatch(YAMLFileType.YML, "bar: \"!php/const:\\YAML_<caret>FOO_BAR\"");
        assertNavigationMatch(YAMLFileType.YML, "bar: '!php/const:\\YAML_<caret>FOO_BAR'");
    }

    public void testPhpConstantNavigation34() {
        assertNavigationMatch(YAMLFileType.YML, "bar: !php/const \\YAML_<caret>FOO_BAR");
        assertNavigationMatch(YAMLFileType.YML, "bar: !php/const YAML_<caret>FOO_BAR");
        assertNavigationMatch(YAMLFileType.YML, "bar: !php/const Yaml\\Foo\\Bar::YAML_FOO_BAR<caret>_CLASS");
        assertNavigationMatch(YAMLFileType.YML, "bar: !php/const Yaml\\Foo\\Bar::::YAML_FOO_BAR<caret>_CLASS");
        assertNavigationMatch(YAMLFileType.YML, "bar: !php/const Yaml\\Foo\\Bar:YAML_FOO_BAR<caret>_CLASS");
        assertNavigationMatch(YAMLFileType.YML, "bar: !php/const \\Yaml\\Foo\\Bar:YAML_FOO_BAR<caret>_CLASS");
    }

    public void testParameter() {
        assertNavigationMatch(YAMLFileType.YML, "bar: %foo_p<caret>arameter%");
    }

    public void testEnvironmentParameter() {
        assertNavigationMatch(YAMLFileType.YML, "bar: %env(FOOBA<caret>R_ENV)%");
        assertNavigationMatch(YAMLFileType.YML, "bar: '%env(FOOBA<caret>R_ENV)%'");
        assertNavigationMatch(YAMLFileType.YML, "bar: '%env(resolve:FOOBA<caret>R_ENV)%'");
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

        assertNavigationMatch("config.yaml", "foobar<caret>_root:\n" +
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

    public void testNavigateToTaggedIteratorServices() {
        assertNavigationMatch(YAMLFileType.YML, "" +
                "App\\HandlerCollection:\n" +
                "   arguments:\n" +
                "       - !tagged_iterator my_nic<caret>e_tag\n",
            PlatformPatterns.psiElement(PhpClass.class)
        );

        assertNavigationMatch(YAMLFileType.YML, "" +
                "App\\HandlerCollection:\n" +
                "   arguments:\n" +
                "       - !tagged_<caret>iterator my_nice_tag\n",
            PlatformPatterns.psiElement(PhpClass.class)
        );

        assertNavigationMatch(YAMLFileType.YML, "" +
                "App\\HandlerCollection:\n" +
                "   arguments:\n" +
                "       - !tagged_<caret>iterator 'my_nice_tag'\n",
            PlatformPatterns.psiElement(PhpClass.class)
        );
    }

    public void testNavigateToTagInsideHash() {
        assertNavigationMatch(YAMLFileType.YML, "" +
                "App\\HandlerCollection:\n" +
                "   arguments:\n" +
                "       - !tagged_it<caret>erator { tag: my_nice_tag, default_priority_method: getPriority }\n",
            PlatformPatterns.psiElement(PhpClass.class)
        );

        assertNavigationMatch(YAMLFileType.YML, "" +
                "App\\HandlerCollection:\n" +
                "   arguments:\n" +
                "       - !tagged_it<caret>erator { tag: 'my_nice_tag', default_priority_method: getPriority }\n",
            PlatformPatterns.psiElement(PhpClass.class)
        );
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
            "    Fo<caret>o\\Bar: ~\n",
            PlatformPatterns.psiElement(PhpClass.class)
        );

        assertNavigationMatch(YAMLFileType.YML, "" +
            "services:\n" +
            "\n" +
            "    _instanceof:\n" +
            "        Fo<caret>o\\Bar:\n" +
            "            tags: ['console.command']",
            PlatformPatterns.psiElement(PhpClass.class));
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

    public void testNavigateForCallsEventMethodIsProvided() {
        assertNavigationMatch("services.yml", "" +
            "services:\n" +
            "    foobar:\n" +
            "       class: Foo\\Bar\n" +
            "       tags:\n" +
            "           - { method: set<caret>Bar }\n" +
            PlatformPatterns.psiElement(PhpClass.class)
        );

        assertNavigationMatch("services.yml", "" +
            "services:\n" +
            "    Foo\\Bar:\n" +
            "       tags:\n" +
            "           - { method: set<caret>Bar }\n" +
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

        assertNavigationMatch("routing.yml", "" +
            "foobar:\n" +
            "    controller: Foo<caret>bar\n" +
            PlatformPatterns.psiElement(Method.class)
        );
    }

    public void testNamedArgumentsNavigationForService() {
        assertNavigationMatch("services.yml", "" +
                        "services:\n" +
                        "    Foo\\Bar:\n" +
                        "       arguments:\n" +
                        "           $<caret>i: ~\n",
                PlatformPatterns.psiElement(Parameter.class)
        );
    }

    public void testNamedArgumentsNavigationForDefaultBinding() {
        assertNavigationMatch("services.yml", "" +
                        "services:\n" +
                        "   _defaults:\n" +
                        "       bind:\n" +
                        "           $<caret>i: ~\n"+
                        "   Foo\\Bar: ~" +
                PlatformPatterns.psiElement(Parameter.class)
        );

        assertNavigationMatch("services.yml", "" +
                "services:\n" +
                "   _defaults:\n" +
                "       bind:\n" +
                "           $<caret>i: ~\n"+
                "   foobar:\n" +
                "       class: Foo\\Bar\n" +
                PlatformPatterns.psiElement(Parameter.class)
        );
    }

    @NotNull
    private PsiElementPattern.Capture<PhpClass> getClassPattern() {
        return PlatformPatterns.psiElement(PhpClass.class);
    }
}
