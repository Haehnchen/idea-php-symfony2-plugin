package fr.adrienbrault.idea.symfony2plugin.tests.lang;

import com.intellij.testFramework.fixtures.InjectionTestFixture;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import static fr.adrienbrault.idea.symfony2plugin.lang.StringLiteralLanguageInjector.LANGUAGE_ID_EXPRESSION_LANGUAGE;

public class StringLiteralLanguageInjectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    private InjectionTestFixture injectionTestFixture;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        injectionTestFixture = new InjectionTestFixture(myFixture);
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/lang/fixtures";
    }

    public void testExpressionLanguageLanguageInjections() {
        assertInjectedLangAtCaret(
            "<?php $expr = new \\Symfony\\Component\\ExpressionLanguage\\Expression('<caret>');",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php \n" +
            "$expr = new \\Symfony\\Component\\ExpressionLanguage\\ExpressionLanguage(); \n" +
            "$expr->evaluate('<caret>');\n",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php \n" +
            "$expr = new \\Symfony\\Component\\ExpressionLanguage\\ExpressionLanguage(); \n" +
            "$expr->compile('<caret>');\n",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php \n" +
            "$expr = new \\Symfony\\Component\\ExpressionLanguage\\ExpressionLanguage(); \n" +
            "$expr->parse('<caret>');\n",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php \n" +
            "$routes = new \\Symfony\\Component\\Routing\\Loader\\Configurator\\RoutingConfigurator();\n" +
            "$routes->condition('<caret>');\n",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php \n" +
            "$routes = new \\Symfony\\Component\\Routing\\Loader\\Configurator\\RoutingConfigurator();\n" +
            "$routes->condition(condition: '<caret>');\n",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
            "\n" +
            "class ExampleController\n" +
            "{\n" +
            "    #[Route(\"/contact\", condition: \"<caret>\")]\n" +
            "    public function contact() {}\n" +
            "}\n",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
            "\n" +
            "class ExampleController\n" +
            "{\n" +
            "    #[Route([], \"/contact\", \"contact\", [], [], [], null, [], [], '<caret>')]\n" +
            "    public function contact() {}\n" +
            "}",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
            "\n" +
            "class ExampleController\n" +
            "{\n" +
            "    /**\n" +
            "     * @Route(\"/contact\", name=\"contact\", condition=\"<caret>\")\n" +
            "     */\n" +
            "    public function contact() {}\n" +
            "}",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Route;\n" +
            "\n" +
            "class ExampleController\n" +
            "{\n" +
            "    /**\n" +
            "     * @Route(\"/contact\", name=\"contact\", condition=\"<caret>\")\n" +
            "     */\n" +
            "    public function contact() {}\n" +
            "}",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Symfony\\Component\\Validator\\Constraints\\Expression;\n" +
            "\n" +
            "#[Expression(\"<caret>\")]\n" +
            "class BlogPost {}\n",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Symfony\\Component\\Validator\\Constraints\\Expression;\n" +
            "\n" +
            "/**\n" +
            " * @Expression(\"<caret>\")\n" +
            " */\n" +
            "class BlogPost {}\n",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Symfony\\Component\\Validator\\Constraints\\Expression;\n" +
            "\n" +
            "/**\n" +
            " * @Expression(expression: \"<caret>\")\n" +
            " */\n" +
            "class BlogPost {}\n",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Symfony\\Component\\Validator\\Constraints\\Expression;\n" +
            "\n" +
            "#[Expression(expression: \"<caret>\")]\n" +
            "class BlogPost {}\n",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Security;\n" +
            "\n" +
            "class ExampleController\n" +
            "{\n" +
            "    #[Security(\"<caret>\")]\n" +
            "    public function contact() {}\n" +
            "}",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Security;\n" +
            "\n" +
            "class ExampleController\n" +
            "{\n" +
            "    #[Security(data: \"<caret>\")]\n" +
            "    public function contact() {}\n" +
            "}",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Security;\n" +
            "\n" +
            "class ExampleController\n" +
            "{\n" +
            "    /**\n" +
            "     * @Security(\"<caret>\")\n" +
            "     */\n" +
            "    public function contact() {}\n" +
            "}",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Security;\n" +
            "\n" +
            "class ExampleController\n" +
            "{\n" +
            "    /**\n" +
            "     * @Security(expression=\"<caret>\")\n" +
            "     */\n" +
            "    public function contact() {}\n" +
            "}",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Cache;\n" +
            "\n" +
            "class ExampleController\n" +
            "{\n" +
            "    #[Cache(lastModified: '<caret>')]\n" +
            "    public function contact() {}\n" +
            "}",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Cache;\n" +
            "\n" +
            "class ExampleController\n" +
            "{\n" +
            "    #[Cache(ETag: '<caret>')]\n" +
            "    public function contact() {}\n" +
            "}",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Cache;\n" +
            "\n" +
            "class ExampleController\n" +
            "{\n" +
            "    /**\n" +
            "     * @Cache(lastModified=\"<caret>\")\n" +
            "     */\n" +
            "    public function contact() {}\n" +
            "}",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Cache;\n" +
            "\n" +
            "class ExampleController\n" +
            "{\n" +
            "    /**\n" +
            "     * @Cache(Etag=\"<caret>\")\n" +
            "     */\n" +
            "    public function contact() {}\n" +
            "}",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Entity;\n" +
            "\n" +
            "class ExampleController\n" +
            "{\n" +
            "    #[Entity('post', expr: '<caret>')]\n" +
            "    public function contact($post) {}\n" +
            "}",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Entity;\n" +
            "\n" +
            "class ExampleController\n" +
            "{\n" +
            "    #[Entity('post', '<caret>')]\n" +
            "    public function contact($post) {}\n" +
            "}",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Entity;\n" +
            "\n" +
            "class ExampleController\n" +
            "{\n" +
            "    /**\n" +
            "     * @Entity('post', expr=\"<caret>\")\n" +
            "     */\n" +
            "    public function contact($post) {}\n" +
            "}",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php $expr = \\Symfony\\Component\\DependencyInjection\\Loader\\Configurator\\expr(\"<caret>\");\n",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );

        assertInjectedLangAtCaret(
            "<?php $expr = \\Symfony\\Component\\DependencyInjection\\Loader\\Configurator\\expr(expression: \"<caret>\");",
            LANGUAGE_ID_EXPRESSION_LANGUAGE
        );
    }

    private void assertInjectedLangAtCaret(@NotNull String configureByText, @NotNull String lang) {
        myFixture.configureByText(PhpFileType.INSTANCE, configureByText);
        injectionTestFixture.assertInjectedLangAtCaret(lang);
    }
}
