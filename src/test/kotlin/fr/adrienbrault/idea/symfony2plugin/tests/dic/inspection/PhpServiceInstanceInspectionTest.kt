package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.inspection.PhpServiceInstanceInspection
 */
class PhpServiceInstanceInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("PhpServiceInstanceInspection.php"))
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("PhpServiceInstanceInspection.xml"))
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/dic/inspection/fixtures"
    }

    private fun arrayConfig(arguments: String): String {
        return "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "return [\n" +
            "    'services' => [\n" +
            "        'Args\\\\Foo' => [\n" +
            "            'arguments' => [" + arguments + "],\n" +
            "        ],\n" +
            "    ],\n" +
            "];\n"
    }

    fun testArrayStylePositionalConstructorArgumentReportsMismatch() {
        assertLocalInspectionContains("test.php",
            arrayConfig("service('args<caret>_bar')"),
            "Expect instance of: Args\\Foo"
        )
    }

    fun testArrayStyleNamedConstructorArgumentReportsMismatch() {
        assertLocalInspectionContains("test.php",
            "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "return [\n" +
            "    'services' => [\n" +
            "        'Args\\\\Foo' => [\n" +
            "            'arguments' => ['\$foo' => service('args<caret>_bar')],\n" +
            "        ],\n" +
            "    ],\n" +
            "];\n",
            "Expect instance of: Args\\Foo"
        )
    }

    fun testArrayStyleRawAtServiceReportsMismatch() {
        assertLocalInspectionContains("test.php",
            arrayConfig("'@args<caret>_bar'"),
            "Expect instance of: Args\\Foo"
        )
    }

    fun testArrayStyleSecondSlotUntypedDoesNotReport() {
        assertLocalInspectionNotContains("test.php",
            arrayConfig("service('args_foo'), service('args<caret>_bar')"),
            "Expect instance of: Args\\Foo"
        )
    }

    fun testArrayStyleThirdSlotReportsMismatch() {
        assertLocalInspectionContains("test.php",
            arrayConfig("service('args_foo'), service('args_foo'), service('args<caret>_bar')"),
            "Expect instance of: Args\\Foo"
        )
    }

    fun testArrayStyleCorrectInstanceDoesNotReport() {
        assertLocalInspectionNotContains("test.php",
            arrayConfig("service('args<caret>_foo')"),
            "Expect instance of: Args\\Foo"
        )
    }

    fun testFluentArgsServiceReportsMismatch() {
        assertLocalInspectionContains("test.php",
            "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "\$container->services()\n" +
            "    ->set('foo', \\Args\\Foo::class)\n" +
            "    ->args([service('args<caret>_bar')]);",
            "Expect instance of: Args\\Foo"
        )
    }

    fun testFluentArgsRefReportsMismatch() {
        assertLocalInspectionContains("test.php",
            "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "\$container->services()\n" +
            "    ->set('foo', \\Args\\Foo::class)\n" +
            "    ->args([ref('args<caret>_bar')]);",
            "Expect instance of: Args\\Foo"
        )
    }

    fun testFluentArgsCorrectInstanceDoesNotReport() {
        assertLocalInspectionNotContains("test.php",
            "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "\$container->services()\n" +
            "    ->set('foo', \\Args\\Foo::class)\n" +
            "    ->args([service('args<caret>_foo')]);",
            "Expect instance of: Args\\Foo"
        )
    }

    fun testArrayStyleClassConstantReportsMismatch() {
        assertLocalInspectionContains("test.php",
            arrayConfig("service(\\Args\\Bar<caret>::class)"),
            "Expect instance of: Args\\Foo"
        )
    }

    fun testArrayStyleClassConstantCorrectInstanceDoesNotReport() {
        assertLocalInspectionNotContains("test.php",
            arrayConfig("service(\\Args\\Foo<caret>::class)"),
            "Expect instance of: Args\\Foo"
        )
    }

    fun testNonServiceContextDoesNotReport() {
        assertLocalInspectionNotContains("test.php",
            "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "service('args<caret>_bar');",
            "Expect instance of: Args\\Foo"
        )
    }

    fun testNonArgumentsArrayKeyDoesNotReport() {
        assertLocalInspectionNotContains("test.php",
            "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "return [\n" +
            "    'services' => [\n" +
            "        'Args\\\\Foo' => [\n" +
            "            'decorates' => 'args<caret>_bar',\n" +
            "        ],\n" +
            "    ],\n" +
            "];\n",
            "Expect instance of: Args\\Foo"
        )
    }
}
