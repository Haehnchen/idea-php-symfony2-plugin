package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.UxTemplateStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.UxTemplateStubIndex
 */
public class UxTemplateStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testThatComponentIsIndexed() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace {\n" +
            "   use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "   #[AsTwigComponent('alert')]\n" +
            "   class Alert {}\n" +
            "\n" +
            "   #[AsTwigComponent(foobar: 'test', name: 'my_alert_foo')]\n" +
            "   class MyAlert {}\n" +
            "\n" +
            "   #[AsTwigComponent]\n" +
            "   class Foobar {}\n" +
            "\n" +
            "   #[AsTwigComponent(template: 'foobar/foo.html.twig')]\n" +
            "   class Template {}\n" +
            "\n" +
            "   #[AsTwigComponent(template: 'form:input:foo')]\n" +
            "   class TemplateAlias {}\n"
        );

        assertIndexContainsKeyWithValue(UxTemplateStubIndex.KEY, "\\Alert", value ->
            "alert".equals(value.name()) && "\\Alert".equals(value.phpClass())
        );

        assertIndexContainsKeyWithValue(UxTemplateStubIndex.KEY, "\\MyAlert", value ->
            "my_alert_foo".equals(value.name()) && "\\MyAlert".equals(value.phpClass())
        );

        assertIndexContainsKeyWithValue(UxTemplateStubIndex.KEY, "\\Foobar", value ->
            value.name() == null && "\\Foobar".equals(value.phpClass())
        );

        assertIndexContainsKeyWithValue(UxTemplateStubIndex.KEY, "\\Template", value ->
            value.name() == null && "\\Template".equals(value.phpClass()) && "foobar/foo.html.twig".equals(value.template())
        );

        assertIndexContainsKeyWithValue(UxTemplateStubIndex.KEY, "\\TemplateAlias", value ->
            value.name() == null && "\\TemplateAlias".equals(value.phpClass()) && "form/input/foo.html.twig".equals(value.template())
        );
    }
}
