package fr.adrienbrault.idea.symfony2plugin.tests.lang;

import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.InjectionTestFixture;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static fr.adrienbrault.idea.symfony2plugin.lang.StringLiteralLanguageInjector.LANGUAGE_ID_CSS;
import static fr.adrienbrault.idea.symfony2plugin.lang.StringLiteralLanguageInjector.LANGUAGE_ID_DQL;
import static fr.adrienbrault.idea.symfony2plugin.lang.StringLiteralLanguageInjector.LANGUAGE_ID_EXPRESSION_LANGUAGE;
import static fr.adrienbrault.idea.symfony2plugin.lang.StringLiteralLanguageInjector.LANGUAGE_ID_JSON;
import static fr.adrienbrault.idea.symfony2plugin.lang.StringLiteralLanguageInjector.LANGUAGE_ID_XPATH;

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

    public void skipTestCssLanguageInjections() {
        // skip as we dont have CSS module in >= 2020 test builds
        String base = "<?php $c = new \\Symfony\\Component\\DomCrawler\\Crawler();\n";
        assertInjectedLangAtCaret(base + "$c->filter('html > bo<caret>dy');", LANGUAGE_ID_CSS);
        assertInjectedLangAtCaret(base + "$c->filter('<caret>');", LANGUAGE_ID_CSS);
        assertInjectedLangAtCaret(base + "$c->children('html > bo<caret>dy');", LANGUAGE_ID_CSS);
        assertInjectedLangAtCaret(base + "$c->children('<caret>');", LANGUAGE_ID_CSS);

        base = "<?php $c = new \\Symfony\\Component\\CssSelector\\CssSelectorConverter();\n";
        assertInjectedLangAtCaret(base + "$c->toXPath('html > bo<caret>dy');", LANGUAGE_ID_CSS);
        assertInjectedLangAtCaret(base + "$c->toXPath('<caret>');", LANGUAGE_ID_CSS);
    }

    public void testXPathLanguageInjections() {
        String base = "<?php $c = new \\Symfony\\Component\\DomCrawler\\Crawler();\n";
        assertInjectedLangAtCaret(base + "$c->filterXPath('//dum<caret>my');", LANGUAGE_ID_XPATH);
        assertInjectedLangAtCaret(base + "$c->filterXPath('<caret>');", LANGUAGE_ID_XPATH);
        assertInjectedLangAtCaret(base + "$c->evaluate('//dum<caret>my');", LANGUAGE_ID_XPATH);
        assertInjectedLangAtCaret(base + "$c->evaluate('<caret>');", LANGUAGE_ID_XPATH);
    }

    public void testJsonLanguageInjections() {
        String base = "<?php \\Symfony\\Component\\HttpFoundation\\";
        assertInjectedLangAtCaret(base + "JsonResponse::fromJsonString('<caret>');", LANGUAGE_ID_JSON);
        assertInjectedLangAtCaret(base + "JsonResponse::fromJsonString('{\"foo\": <caret>}');", LANGUAGE_ID_JSON);

        base = "<?php $r = new \\Symfony\\Component\\HttpFoundation\\JsonResponse();\n";
        assertInjectedLangAtCaret(base + "$r->setJson('<caret>');", LANGUAGE_ID_JSON);
        assertInjectedLangAtCaret(base + "$r->setJson('{\"foo\": <caret>}');", LANGUAGE_ID_JSON);
    }

    public void testDqlLanguageInjections() {
        String base = "<?php $em = new \\Doctrine\\ORM\\EntityManager();\n";
        assertInjectedLangAtCaret(base + "$em->createQuery('SELECT b FR<caret>OM \\Foo\\Bar b');", LANGUAGE_ID_DQL);
        assertInjectedLangAtCaret(base + "$em->createQuery('<caret>');", LANGUAGE_ID_DQL);
        assertInjectedLangAtCaret(base + "$em->createQuery(<<caret><<AAA\n \nAAA\n);", null);
        assertInjectedFragmentText(base + "$em->createQuery(<<<AAA\nSELEC<caret>T\nAAA\n);", "SELECT");
        assertInjectedFragmentText(base + "$em->createQuery(<<<'AAA'\nSELEC<caret>T a\nAAA\n);", "SELECT a");
        base = "<?php $q = new \\Doctrine\\ORM\\Query();\n";
        assertInjectedLangAtCaret(base + "$q->setDQL('SELECT b FR<caret>OM \\Foo\\Bar b');", LANGUAGE_ID_DQL);
        assertInjectedLangAtCaret(base + "$q->setDQL('<caret>');", LANGUAGE_ID_DQL);

        assertInjectedLangAtCaret("<?php $dql = \"SELECT b FR<caret>OM \\Foo\\Bar b\");", LANGUAGE_ID_DQL);
        assertInjectedLangAtCaret("<?php $dql = \"<caret>\");", LANGUAGE_ID_DQL);
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
            "new Expression(['expression' => '<caret>']);\n",
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

    private void assertInjectedLangAtCaret(@NotNull String configureByText, @Nullable String lang) {
        myFixture.configureByText(PhpFileType.INSTANCE, configureByText);
        injectionTestFixture.assertInjectedLangAtCaret(lang);
    }

    private void assertInjectedFragmentText(@NotNull String configureByText, String text) {
        myFixture.configureByText(PhpFileType.INSTANCE, configureByText);
        PsiElement injectedElement = injectionTestFixture.getInjectedElement();
        assertNotNull(injectedElement);
        TestCase.assertEquals(text, injectedElement.getContainingFile().getText());
    }
}
