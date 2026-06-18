package fr.adrienbrault.idea.symfony2plugin.tests.config.php;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @see fr.adrienbrault.idea.symfony2plugin.config.php.PhpArrayGotoCompletionRegistrar
 */
public class PhpArrayGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject("src/Service/Mailer.php", "<?php\nnamespace App\\Service;\nclass Mailer {}\n");
        myFixture.addFileToProject("src/Service/DecoratingMailer.php", "<?php\nnamespace App\\Service;\nclass DecoratingMailer extends Mailer {}\n");
        myFixture.addFileToProject("config/parameters.yaml", "parameters:\n    mailer.transport: smtp\n");
        myFixture.addFileToProject("config/php_array_targets.php", "<?php\nnamespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\nreturn App::config([\n    'services' => [\n        'app.mailer' => ['class' => \\App\\Service\\Mailer::class],\n    ],\n]);\n");
    }

    public void testDecoratesCompletion() {
        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.decorator' => [\n" +
                "            'class' => \\App\\Service\\DecoratingMailer::class,\n" +
                "            'decorates' => '<caret>',\n" +
                "        ],\n" +
                "    ],\n" +
                "];",
            "app.mailer"
        );
    }

    public void testParentCompletion() {
        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.decorator' => [\n" +
                "            'class' => \\App\\Service\\DecoratingMailer::class,\n" +
                "            'parent' => '<caret>',\n" +
                "        ],\n" +
                "    ],\n" +
                "];",
            "app.mailer"
        );
    }

    public void testDecoratesCompletionPrioritizesMatchingService() {
        assertCompletionLookupContainsPresentableItem(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.decorator' => [\n" +
                "            'class' => \\App\\Service\\DecoratingMailer::class,\n" +
                "            'decorates' => '<caret>',\n" +
                "        ],\n" +
                "    ],\n" +
                "];",
            lookupElement -> "app.mailer".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold()
        );
    }

    public void testArgumentsParameterCompletion() {
        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.foo' => [\n" +
                "            'arguments' => ['%<caret>'],\n" +
                "        ],\n" +
                "    ],\n" +
                "];",
            "%mailer.transport%"
        );
    }

    public void testCallsArgumentParameterCompletion() {
        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.foo' => [\n" +
                "            'calls' => [['setTransport', ['%<caret>']]],\n" +
                "        ],\n" +
                "    ],\n" +
                "];",
            "%mailer.transport%"
        );
    }

    public void testArgumentsParameterNavigation() {
        assertNavigationMatch(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.foo' => [\n" +
                "            'arguments' => ['%mailer.trans<caret>port%'],\n" +
                "        ],\n" +
                "    ],\n" +
                "];"
        );
    }

    public void testCallsArgumentParameterNavigation() {
        assertNavigationMatch(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.foo' => [\n" +
                "            'calls' => [['setTransport', ['%mailer.trans<caret>port%']]],\n" +
                "        ],\n" +
                "    ],\n" +
                "];"
        );
    }
}
