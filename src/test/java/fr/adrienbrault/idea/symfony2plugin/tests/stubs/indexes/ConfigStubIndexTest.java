package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ConfigStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConfigStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/indexes/fixtures";
    }

    public void testIndexingForConfigValues() {
        myFixture.copyFileToProject("twig_component.yaml");

        assertIndexContains(ConfigStubIndex.KEY, "anonymous_template_directory");

        assertIndexContainsKeyWithValue(
            ConfigStubIndex.KEY,
            "anonymous_template_directory",
            value -> "anonymous_template_directory".equals(value.getName()) && value.getValues().contains("components/")
        );

        assertIndexContains(ConfigStubIndex.KEY, "twig_component_defaults");

        assertIndexContainsKeyWithValue(
            ConfigStubIndex.KEY,
            "twig_component_defaults",
            value -> value.getConfigs().get("App\\Twig\\Components2\\").get("template_directory").contains("components")
        );
    }

    public void testIndexingForPhpRootArrayConfigValues() {
        myFixture.addFileToProject("config/packages/twig_component_php.php", "<?php\n" +
            "return [\n" +
            "    'twig_component' => [\n" +
            "        'defaults' => [\n" +
            "            'App\\\\PhpShort\\\\Components\\\\' => 'php/components',\n" +
            "            'App\\\\PhpLong\\\\Components\\\\' => [\n" +
            "                'template_directory' => 'php/long',\n" +
            "                'name_prefix' => 'PhpLong',\n" +
            "            ],\n" +
            "            'App\\\\PhpDefaulted\\\\Components\\\\' => [\n" +
            "                'name_prefix' => '',\n" +
            "            ],\n" +
            "        ],\n" +
            "        'anonymous_template_directory' => 'php_components',\n" +
            "    ],\n" +
            "];\n"
        );

        assertIndexContainsKeyWithValue(
            ConfigStubIndex.KEY,
            "twig_component_defaults",
            value -> value.getConfigs().containsKey("App\\PhpShort\\Components\\") &&
                "php/components".equals(value.getConfigs().get("App\\PhpShort\\Components\\").get("template_directory")) &&
                "php/long".equals(value.getConfigs().get("App\\PhpLong\\Components\\").get("template_directory")) &&
                "PhpLong".equals(value.getConfigs().get("App\\PhpLong\\Components\\").get("name_prefix")) &&
                "components".equals(value.getConfigs().get("App\\PhpDefaulted\\Components\\").get("template_directory")) &&
                "".equals(value.getConfigs().get("App\\PhpDefaulted\\Components\\").get("name_prefix"))
        );

        assertIndexContainsKeyWithValue(
            ConfigStubIndex.KEY,
            "anonymous_template_directory",
            value -> value.getValues().contains("php_components")
        );
    }

    public void testIndexingForPhpWhenConfigValues() {
        myFixture.addFileToProject("config/packages/twig_component_when.php", "<?php\n" +
            "return [\n" +
            "    'when@test' => [\n" +
            "        'twig_component' => [\n" +
            "            'defaults' => [\n" +
            "                'App\\\\PhpWhen\\\\Components\\\\' => 'php/when',\n" +
            "            ],\n" +
            "        ],\n" +
            "    ],\n" +
            "];\n"
        );

        assertIndexContainsKeyWithValue(
            ConfigStubIndex.KEY,
            "twig_component_defaults",
            value -> value.getConfigs().containsKey("App\\PhpWhen\\Components\\") &&
                "php/when".equals(value.getConfigs().get("App\\PhpWhen\\Components\\").get("template_directory"))
        );
    }

    public void testIndexingForPhpAppConfigValues() {
        myFixture.addFileToProject("config/packages/twig_component_app_config.php", "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "\n" +
            "return App::config([\n" +
            "    'twig_component' => [\n" +
            "        'defaults' => [\n" +
            "            'App\\\\PhpAppConfig\\\\Components\\\\' => 'php/app-config',\n" +
            "        ],\n" +
            "    ],\n" +
            "]);\n"
        );

        assertIndexContainsKeyWithValue(
            ConfigStubIndex.KEY,
            "twig_component_defaults",
            value -> value.getConfigs().containsKey("App\\PhpAppConfig\\Components\\") &&
                "php/app-config".equals(value.getConfigs().get("App\\PhpAppConfig\\Components\\").get("template_directory"))
        );
    }

    public void testIndexingForPhpAppConfigWhenValues() {
        myFixture.addFileToProject("config/packages/twig_component_app_config_when.php", "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "\n" +
            "return App::config([\n" +
            "    'when@test' => [\n" +
            "        'twig_component' => [\n" +
            "            'defaults' => [\n" +
            "                'App\\\\PhpAppConfigWhen\\\\Components\\\\' => 'php/app-config-when',\n" +
            "            ],\n" +
            "        ],\n" +
            "    ],\n" +
            "]);\n"
        );

        assertIndexContainsKeyWithValue(
            ConfigStubIndex.KEY,
            "twig_component_defaults",
            value -> value.getConfigs().containsKey("App\\PhpAppConfigWhen\\Components\\") &&
                "php/app-config-when".equals(value.getConfigs().get("App\\PhpAppConfigWhen\\Components\\").get("template_directory"))
        );
    }

    public void testIndexingForPhpAppConfigStatementValues() {
        myFixture.addFileToProject("config/packages/twig_component_app_config_statement.php", "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "\n" +
            "App::config([\n" +
            "    'twig_component' => [\n" +
            "        'defaults' => [\n" +
            "            'App\\\\PhpAppConfigStatement\\\\Components\\\\' => 'php/app-config-statement',\n" +
            "        ],\n" +
            "    ],\n" +
            "]);\n"
        );

        assertIndexContainsKeyWithValue(
            ConfigStubIndex.KEY,
            "twig_component_defaults",
            value -> value.getConfigs().containsKey("App\\PhpAppConfigStatement\\Components\\") &&
                "php/app-config-statement".equals(value.getConfigs().get("App\\PhpAppConfigStatement\\Components\\").get("template_directory"))
        );
    }

    public void testIndexingForPhpContainerExtensionConfigValues() {
        myFixture.addFileToProject("config/packages/twig_component_extension.php", "<?php\n" +
            "use Symfony\\Component\\DependencyInjection\\Loader\\Configurator\\ContainerConfigurator;\n" +
            "\n" +
            "return static function (ContainerConfigurator $container): void {\n" +
            "    $container->extension('twig_component', [\n" +
            "        'defaults' => [\n" +
            "            'App\\\\Shared\\\\Ui\\\\Web\\\\Component\\\\' => [\n" +
            "                'template_directory' => '@Shared',\n" +
            "            ],\n" +
            "        ],\n" +
            "        'anonymous_template_directory' => 'components',\n" +
            "    ]);\n" +
            "};\n"
        );

        assertIndexContainsKeyWithValue(
            ConfigStubIndex.KEY,
            "twig_component_defaults",
            value -> value.getConfigs().containsKey("App\\Shared\\Ui\\Web\\Component\\") &&
                "@Shared".equals(value.getConfigs().get("App\\Shared\\Ui\\Web\\Component\\").get("template_directory"))
        );

        assertIndexContainsKeyWithValue(
            ConfigStubIndex.KEY,
            "anonymous_template_directory",
            value -> value.getValues().contains("components")
        );
    }

    public void testIndexingIgnoresClosureInsideReturnedArray() {
        myFixture.addFileToProject("config/packages/twig_component_nested_closure.php", "<?php\n" +
            "return [\n" +
            "    'not_config' => static function ($container): void {\n" +
            "        $container->extension('twig_component', [\n" +
            "            'defaults' => [\n" +
            "                'App\\\\NestedClosure\\\\Components\\\\' => 'nested/closure',\n" +
            "            ],\n" +
            "        ]);\n" +
            "    },\n" +
            "];\n"
        );

        assertIndexNotContains(ConfigStubIndex.KEY, "twig_component_defaults", "anonymous_template_directory");
    }
}
