package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpTwigTemplateUsageStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpTwigTemplateUsageStubIndex
 */
public class PhpTwigTemplateUsageStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testThatTwigRenderMethodsAreInIndex() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "class Foo\n" +
            "{\n" +
            "   const FOO = 'const.html.twig';\n" +
            "   const FOO_TERNARY = 'const-ternary.html.twig';\n" +
            "   const FOO_COALESCE = 'const-coalesce.html.twig';\n" +
            "   private $foo = 'private.html.twig';\n" +
            "   public function foobar($defaultParameter = 'default-function-parameter.html.twig') {\n" +
            "       $var = 'var.html.twig';\n" +
            "       $foo->render('foo-render.html.twig');\n" +
            "       $foo->render('foo-render.html.twig');\n" +
            "       $foo->render('foobar-render.twig');\n" +
            "       $foo->render(true === true ? 'foo-render-ternary.html.twig' : self::FOO_TERNARY);\n" +
            "       $foo->render($foobar ?? 'foo-render-coalesce.html.twig');\n" +
            "       $foo->render($foobar ?? self::FOO_COALESCE);\n" +
            "       $foo->render('@!Foo/overwrite.html.twig');\n" +
            "       $foo->renderView('foo-renderView.html.twig');\n" +
            "       $foo->renderView($t = 'foo-var-assignment-expression.html.twig');\n" +
            "       $foo->renderView($t = $foo->foo());\n" +
            "       $foo->renderResponse('foo-renderResponse.html.twig');\n" +
            "       $foo->render(self::FOO);\n" +
            "       $foo->render($var);\n" +
            "       $foo->render($this->foo);\n" +
            "       $foo->render(\\DateTime::foo);\n" +
            "       $foo->renderView($defaultParameter);\n" +
            "       $foo1->htmlTemplate('emails/signup.html.twig');\n" +
            "       $foo1->textTemplate('emails/signup.txt.twig');\n" +
            "   }\n" +
            "}" +
            "\n" +
            "function foobarFunc()" +
            "{\n" +
            "   $foo->render('foo-render.html.twig')\n" +
            "}\n"
        );

        assertIndexContains(
            PhpTwigTemplateUsageStubIndex.KEY,
            "foo-render.html.twig", "foo-renderView.html.twig", "foo-renderResponse.html.twig",
            "@Foo/overwrite.html.twig", "const.html.twig", "var.html.twig", "private.html.twig", "foobar-render.twig",
            "foo-render-ternary.html.twig", "const-ternary.html.twig", "foo-render-coalesce.html.twig", "const-coalesce.html.twig",
            "foo-var-assignment-expression.html.twig", "default-function-parameter.html.twig", "emails/signup.html.twig", "emails/signup.txt.twig"
        );

        assertIndexContainsKeyWithValue(PhpTwigTemplateUsageStubIndex.KEY, "foo-render.html.twig", value ->
            "foo-render.html.twig".equals(value.getTemplate()) && value.getScopes().contains("Foo.foobar")
        );

        assertIndexContainsKeyWithValue(PhpTwigTemplateUsageStubIndex.KEY, "foo-render.html.twig", value ->
            "foo-render.html.twig".equals(value.getTemplate()) && value.getScopes().contains("foobarFunc")
        );
    }

    public void testThatAdditionalTwigRenderCallsAreInIndex() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "final class PageController\n" +
            "{\n" +
            "   public function index(bool $preview, ?string $templateName): void {\n" +
            "       $template = 'page/local.html.twig';\n" +
            "       $this->render('page/direct.html.twig');\n" +
            "       $this->render(view: 'page/named.html.twig', parameters: []);\n" +
            "       $this->render(parameters: [], view: 'page/reordered.html.twig');\n" +
            "       $this->renderView('fragment/direct.html.twig');\n" +
            "       $this->renderView(view: 'fragment/named.html.twig', parameters: []);\n" +
            "       $this->renderBlock('page/block-direct.html.twig', 'content', []);\n" +
            "       $this->renderBlock(view: 'page/block-named.html.twig', block: 'content', parameters: []);\n" +
            "       $this->renderBlock(block: 'content-block.html.twig', parameters: [], view: 'page/block-reordered.html.twig');\n" +
            "       $this->renderBlockView('page/block-view-direct.html.twig', 'content', []);\n" +
            "       $this->renderBlockView(view: 'page/block-view-named.html.twig', block: 'content', parameters: []);\n" +
            "       $this->renderBlockView(block: 'content-block-view.html.twig', parameters: [], view: 'page/block-view-reordered.html.twig');\n" +
            "       $this->render($template);\n" +
            "       $this->render($preview ? 'page/preview.html.twig' : 'page/index.html.twig');\n" +
            "       $this->render($templateName ?? 'page/fallback.html.twig');\n" +
            "   }\n" +
            "}\n" +
            "\n" +
            "final class TemplateRenderer\n" +
            "{\n" +
            "   public function render($twig): void {\n" +
            "       $twig->render('widget/card.html.twig', []);\n" +
            "       $twig->render(name: 'widget/card-named.html.twig', context: []);\n" +
            "       $twig->render(context: [], name: 'widget/card-reordered.html.twig');\n" +
            "   }\n" +
            "}\n" +
            "\n" +
            "final class MailFactory\n" +
            "{\n" +
            "   public function create(): void {\n" +
            "       $email = new TemplatedEmail();\n" +
            "       $email->htmlTemplate('email/direct.html.twig');\n" +
            "       $email->htmlTemplate(template: 'email/named.html.twig');\n" +
            "       $email->textTemplate('email/direct.txt.twig');\n" +
            "       $email->textTemplate(template: 'email/named.txt.twig');\n" +
            "   }\n" +
            "}\n"
        );

        assertIndexContains(
            PhpTwigTemplateUsageStubIndex.KEY,
            "page/direct.html.twig",
            "page/named.html.twig",
            "page/reordered.html.twig",
            "fragment/direct.html.twig",
            "fragment/named.html.twig",
            "page/block-direct.html.twig",
            "page/block-named.html.twig",
            "page/block-reordered.html.twig",
            "page/block-view-direct.html.twig",
            "page/block-view-named.html.twig",
            "page/block-view-reordered.html.twig",
            "page/local.html.twig",
            "page/preview.html.twig",
            "page/index.html.twig",
            "page/fallback.html.twig",
            "widget/card.html.twig",
            "widget/card-named.html.twig",
            "widget/card-reordered.html.twig",
            "email/direct.html.twig",
            "email/named.html.twig",
            "email/direct.txt.twig",
            "email/named.txt.twig"
        );

        assertIndexContainsKeyWithValue(PhpTwigTemplateUsageStubIndex.KEY, "page/named.html.twig", value ->
            "page/named.html.twig".equals(value.getTemplate()) && value.getScopes().contains("PageController.index")
        );

        assertIndexContainsKeyWithValue(PhpTwigTemplateUsageStubIndex.KEY, "widget/card-named.html.twig", value ->
            "widget/card-named.html.twig".equals(value.getTemplate()) && value.getScopes().contains("TemplateRenderer.render")
        );

        assertIndexContainsKeyWithValue(PhpTwigTemplateUsageStubIndex.KEY, "email/named.html.twig", value ->
            "email/named.html.twig".equals(value.getTemplate()) && value.getScopes().contains("MailFactory.create")
        );

        assertIndexNotContains(
            PhpTwigTemplateUsageStubIndex.KEY,
            "content-block.html.twig",
            "content-block-view.html.twig"
        );
    }

    public void testThatDefaultTemplatePropertyAnnotationIsIndexed() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;" +
            "class Foobar\n" +
            "{" +
            "/**\n" +
            " *\n" +
            " * @Template(\"foo-annotation-default.html.twig\")\n" +
            " */" +
            "public function foobar() {}" +
            "}\n"
        );

        assertIndexContains(
            PhpTwigTemplateUsageStubIndex.KEY,
            "foo-annotation-default.html.twig"
        );

        assertIndexContainsKeyWithValue(PhpTwigTemplateUsageStubIndex.KEY, "foo-annotation-default.html.twig", value ->
            "foo-annotation-default.html.twig".equals(value.getTemplate()) && value.getScopes().contains("Foobar.foobar")
        );
    }

    public void testThatTemplatePropertyAnnotationIsIndexed() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;" +
            "class Foobar\n" +
            "{" +
            "/**\n" +
            " *\n" +
            " * @Template(foo=\"bar\", template=\"foo-annotation-property.html.twig\")\n" +
            " * @Template(foo=\"bar\", template=\"@!Bar/overwrite.html.twig\")\n" +
            " */" +
            "public function foobar() {}" +
            "}\n"
        );

        assertIndexContains(
            PhpTwigTemplateUsageStubIndex.KEY,
            "foo-annotation-property.html.twig", "@Bar/overwrite.html.twig"
        );

        assertIndexContainsKeyWithValue(PhpTwigTemplateUsageStubIndex.KEY, "foo-annotation-property.html.twig", value ->
            "foo-annotation-property.html.twig".equals(value.getTemplate()) && value.getScopes().contains("Foobar.foobar")
        );
    }

    public void testEmptyTemplateAnnotationIndexUsingTemplateGuesser() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;" +
            "" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;" +
            "class MyNiceFoobarController\n" +
            "{" +
            "/**\n" +
            " *\n" +
            " * @Template()\n" +
            " */" +
            "public function foobarWhatAction() {}" +
            "}\n"
        );

        assertIndexContains(
            PhpTwigTemplateUsageStubIndex.KEY,
            "my_nice_foobar/foobar_what.html.twig"
        );
    }


    public void testThatDefaultTemplatePropertyPhpAttributeIsIndexed() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;" +
            "class Foobar\n" +
            "{" +
            "#[Template(\"foo-php-attribute-default.html.twig\")]\n" +
            "public function foobar() {}" +
            "}\n"
        );

        assertIndexContains(
            PhpTwigTemplateUsageStubIndex.KEY,
            "foo-php-attribute-default.html.twig"
        );

        assertIndexContainsKeyWithValue(PhpTwigTemplateUsageStubIndex.KEY, "foo-php-attribute-default.html.twig", value ->
            "foo-php-attribute-default.html.twig".equals(value.getTemplate()) && value.getScopes().contains("Foobar.foobar")
        );
    }

    public void testThatDefaultTemplatePropertyPhpAttributeTemplateIsIndexed() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;" +
            "class Foobar\n" +
            "{" +
            "#[Template(template: \"foo-php-attribute-template.html.twig\")]\n" +
            "public function foobar() {}" +
            "}\n"
        );

        assertIndexContains(
            PhpTwigTemplateUsageStubIndex.KEY,
            "foo-php-attribute-template.html.twig"
        );

        assertIndexContainsKeyWithValue(PhpTwigTemplateUsageStubIndex.KEY, "foo-php-attribute-template.html.twig", value ->
            "foo-php-attribute-template.html.twig".equals(value.getTemplate()) && value.getScopes().contains("Foobar.foobar")
        );
    }

    public void testEmptyTemplatePhpAttributeIndexUsingTemplateGuesser() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;" +
            "" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;" +
            "class MyNiceFoobarController\n" +
            "{" +
            "#[Template()]\n" +
            "public function foobarWhatAction() {}" +
            "}\n"
        );

        assertIndexContains(
            PhpTwigTemplateUsageStubIndex.KEY,
            "my_nice_foobar/foobar_what.html.twig"
        );
    }

    public void testEmptyTemplateAnnotationIndexUsingInvokeTemplateGuesser() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;" +
            "" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;" +
            "class MyNiceFoobarController\n" +
            "{" +
            "/**\n" +
            " *\n" +
            " * @Template()\n" +
            " */" +
            "public function __invoke() {}" +
            "}\n"
        );

        assertIndexContains(
            PhpTwigTemplateUsageStubIndex.KEY,
            "my_nice_foobar.html.twig"
        );
    }

    public void testEmptyTemplateAnnotationIndexWithDirectoryUseTemplateGuesser() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller\\CarItem\\WithApple;" +
            "" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;" +
            "class MyNiceFoobarController\n" +
            "{" +
            "/**\n" +
            " *\n" +
            " * @Template()\n" +
            " */" +
            "public function foobarWhatAction() {}" +
            "}\n"
        );

        assertIndexContains(
            PhpTwigTemplateUsageStubIndex.KEY,
            "car_item/with_apple/my_nice_foobar/foobar_what.html.twig"
        );
    }
}
