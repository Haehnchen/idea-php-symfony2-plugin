package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.inspection.PhpServiceInstanceInspection
 */
public class PhpServiceInstanceInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("PhpServiceInstanceInspection.php"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("PhpServiceInstanceInspection.xml"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/inspection/fixtures";
    }

    private static String arrayConfig(String arguments) {
        return "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "return [\n" +
            "    'services' => [\n" +
            "        'Args\\\\Foo' => [\n" +
            "            'arguments' => [" + arguments + "],\n" +
            "        ],\n" +
            "    ],\n" +
            "];\n";
    }

    public void testArrayStylePositionalConstructorArgumentReportsMismatch() {
        assertLocalInspectionContains("test.php",
            arrayConfig("service('args<caret>_bar')"),
            "Expect instance of: Args\\Foo"
        );
    }

    public void testArrayStyleNamedConstructorArgumentReportsMismatch() {
        assertLocalInspectionContains("test.php",
            "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "return [\n" +
            "    'services' => [\n" +
            "        'Args\\\\Foo' => [\n" +
            "            'arguments' => ['$foo' => service('args<caret>_bar')],\n" +
            "        ],\n" +
            "    ],\n" +
            "];\n",
            "Expect instance of: Args\\Foo"
        );
    }

    public void testArrayStyleRawAtServiceReportsMismatch() {
        assertLocalInspectionContains("test.php",
            arrayConfig("'@args<caret>_bar'"),
            "Expect instance of: Args\\Foo"
        );
    }

    public void testArrayStyleSecondSlotUntypedDoesNotReport() {
        assertLocalInspectionNotContains("test.php",
            arrayConfig("service('args_foo'), service('args<caret>_bar')"),
            "Expect instance of: Args\\Foo"
        );
    }

    public void testArrayStyleThirdSlotReportsMismatch() {
        assertLocalInspectionContains("test.php",
            arrayConfig("service('args_foo'), service('args_foo'), service('args<caret>_bar')"),
            "Expect instance of: Args\\Foo"
        );
    }

    public void testArrayStyleCorrectInstanceDoesNotReport() {
        assertLocalInspectionNotContains("test.php",
            arrayConfig("service('args<caret>_foo')"),
            "Expect instance of: Args\\Foo"
        );
    }

    public void testFluentArgsServiceReportsMismatch() {
        assertLocalInspectionContains("test.php",
            "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "$container->services()\n" +
            "    ->set('foo', \\Args\\Foo::class)\n" +
            "    ->args([service('args<caret>_bar')]);",
            "Expect instance of: Args\\Foo"
        );
    }

    public void testFluentArgsRefReportsMismatch() {
        assertLocalInspectionContains("test.php",
            "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "$container->services()\n" +
            "    ->set('foo', \\Args\\Foo::class)\n" +
            "    ->args([ref('args<caret>_bar')]);",
            "Expect instance of: Args\\Foo"
        );
    }

    public void testFluentArgsCorrectInstanceDoesNotReport() {
        assertLocalInspectionNotContains("test.php",
            "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "$container->services()\n" +
            "    ->set('foo', \\Args\\Foo::class)\n" +
            "    ->args([service('args<caret>_foo')]);",
            "Expect instance of: Args\\Foo"
        );
    }

    public void testArrayStyleClassConstantReportsMismatch() {
        assertLocalInspectionContains("test.php",
            arrayConfig("service(\\Args\\Bar<caret>::class)"),
            "Expect instance of: Args\\Foo"
        );
    }

    public void testArrayStyleClassConstantCorrectInstanceDoesNotReport() {
        assertLocalInspectionNotContains("test.php",
            arrayConfig("service(\\Args\\Foo<caret>::class)"),
            "Expect instance of: Args\\Foo"
        );
    }

    public void testNonServiceContextDoesNotReport() {
        assertLocalInspectionNotContains("test.php",
            "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "service('args<caret>_bar');",
            "Expect instance of: Args\\Foo"
        );
    }

    public void testNonArgumentsArrayKeyDoesNotReport() {
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
        );
    }
}
