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
            "   public function foobar() {\n" +
            "       $foo->render('foo-render.html.twig');\n" +
            "       $foo->renderView('foo-renderView.html.twig');\n" +
            "       $foo->renderResponse('foo-renderResponse.html.twig');\n" +
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
            "foo-render.html.twig", "foo-renderView.html.twig", "foo-renderResponse.html.twig"
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
            " */" +
            "public function foobar() {}" +
            "}\n"
        );

        assertIndexContains(
            PhpTwigTemplateUsageStubIndex.KEY,
            "foo-annotation-property.html.twig"
        );

        assertIndexContainsKeyWithValue(PhpTwigTemplateUsageStubIndex.KEY, "foo-annotation-property.html.twig", value ->
            "foo-annotation-property.html.twig".equals(value.getTemplate()) && value.getScopes().contains("Foobar.foobar")
        );
    }
}
