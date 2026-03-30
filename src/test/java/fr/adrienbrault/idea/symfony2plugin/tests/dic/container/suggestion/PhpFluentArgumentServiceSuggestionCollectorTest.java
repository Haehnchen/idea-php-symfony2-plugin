package fr.adrienbrault.idea.symfony2plugin.tests.dic.container.suggestion;

import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.PhpFluentArgumentServiceSuggestionCollector;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.PhpFluentArgumentServiceSuggestionCollector
 */
public class PhpFluentArgumentServiceSuggestionCollectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("services.xml");
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/container/suggestion/fixtures";
    }

    public void testFluentArgsServiceCall() {
        assertCompletionLookupContainsPresentableItem(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return static function (ContainerConfigurator $container) {\n" +
                "    $container->services()\n" +
                "        ->set('foo', \\Foo\\Bar\\Car::class)\n" +
                "        ->args([service('<caret>')]);\n" +
                "};\n",
            lookupElement -> "foo_bar_apple".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold()
        );
    }

    public void testFluentArgsRefCall() {
        assertCompletionLookupContainsPresentableItem(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return static function (ContainerConfigurator $container) {\n" +
                "    $container->services()\n" +
                "        ->set('foo', \\Foo\\Bar\\Car::class)\n" +
                "        ->args([ref('<caret>')]);\n" +
                "};\n",
            lookupElement -> "foo_bar_apple".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold()
        );
    }

    public void testFluentArgsSecondSlotResolvesCorrectly() {
        assertCompletionLookupContainsPresentableItem(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return static function (ContainerConfigurator $container) {\n" +
                "    $container->services()\n" +
                "        ->set('foo', \\Foo\\Bar\\Car::class)\n" +
                "        ->args([service('x'), service('<caret>')]);\n" +
                "};\n",
            lookupElement -> "foo_bar_car".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold()
        );
    }

    public void testServiceCallOutsideFluentArgsDoesNotPrioritize() {
        StringLiteralExpression element = PhpPsiElementFactory.createFromText(getProject(), StringLiteralExpression.class,
            "<?php\n" +
            "service('foo');\n"
        );

        assert element != null;

        Collection<String> suggestions = new PhpFluentArgumentServiceSuggestionCollector().collect(
            element,
            ContainerCollectionResolver.getServices(getProject()).values()
        );
        assertEmpty(suggestions);
    }
}
