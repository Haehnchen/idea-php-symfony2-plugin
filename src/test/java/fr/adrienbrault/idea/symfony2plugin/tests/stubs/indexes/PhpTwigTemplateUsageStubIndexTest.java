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
            "   }\n" +
            "}" +
            "" +
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
            "foo-var-assignment-expression.html.twig", "default-function-parameter.html.twig"
        );

        assertIndexContainsKeyWithValue(PhpTwigTemplateUsageStubIndex.KEY, "foo-render.html.twig", value ->
            "foo-render.html.twig".equals(value.getTemplate()) && value.getScopes().contains("Foo.foobar")
        );

        assertIndexContainsKeyWithValue(PhpTwigTemplateUsageStubIndex.KEY, "foo-render.html.twig", value ->
            "foo-render.html.twig".equals(value.getTemplate()) && value.getScopes().contains("foobarFunc")
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
