package fr.adrienbrault.idea.symfony2plugin.tests.dic.container.suggestion;

import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.PhpArrayArgumentServiceSuggestionCollector;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.PhpArrayArgumentServiceSuggestionCollector
 */
public class PhpArrayArgumentServiceSuggestionCollectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("services.xml");
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/container/suggestion/fixtures";
    }

    public void testPositionalConstructorArgument() {
        assertCompletionLookupContainsPresentableItem(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'Foo\\\\Bar\\\\Car' => [\n" +
                "            'arguments' => [service('<caret>')],\n" +
                "        ],\n" +
                "    ],\n" +
                "];\n",
            lookupElement -> "foo_bar_apple".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold()
        );
    }

    public void testNamedConstructorArgument() {
        assertCompletionLookupContainsPresentableItem(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'Foo\\\\Bar\\\\Car' => [\n" +
                "            'arguments' => ['$apple' => service('<caret>')],\n" +
                "        ],\n" +
                "    ],\n" +
                "];\n",
            lookupElement -> "foo_bar_apple".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold()
        );
    }

    public void testRawAtStringInsideArguments() {
        assertCompletionLookupContainsPresentableItem(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'Foo\\\\Bar\\\\Car' => [\n" +
                "            'arguments' => ['@<caret>'],\n" +
                "        ],\n" +
                "    ],\n" +
                "];\n",
            lookupElement -> "foo_bar_apple".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold()
        );
    }

    public void testNonArgumentKeyDoesNotPrioritize() {
        StringLiteralExpression element = PhpPsiElementFactory.createFromText(getProject(), StringLiteralExpression.class,
            "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "return [\n" +
            "    'services' => [\n" +
            "        'Foo\\Bar\\Car' => [\n" +
            "            'decorates' => 'foo',\n" +
            "        ],\n" +
            "    ],\n" +
            "];\n"
        );

        assert element != null;

        Collection<String> suggestions = new PhpArrayArgumentServiceSuggestionCollector().collect(
            element,
            ContainerCollectionResolver.getServices(getProject()).values()
        );
        assertEmpty(suggestions);
    }
}
