package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyServiceLocatorCollector

class SymfonyServiceLocatorCollectorTest : McpCollectorTestCase() {
    fun testCollectResolvesByServiceNameAndClass() {
        val byServiceName = SymfonyServiceLocatorCollector(project).collect("foo.yml_id")
        assertEquals(
            """
            ## foo.yml_id
            
            File: config/services.yml
            5:     foo.yml_id:
            6:         class: My\Foo\Service\Targets
            7:         parent: foo.yml_id.parent
            """.trimIndent(),
            byServiceName.trim()
        )
        assertUsesRealLineBreaks(byServiceName)

        val byClassName = SymfonyServiceLocatorCollector(project).collect("My\\Foo\\Service\\Targets")
        assertEquals(
            """
            ## foo.xml_id
            
            File: config/services.xml
            8:         <service id="foo.xml_id" class="My\Foo\Service\Targets" parent="foo.xml_id.parent"/>
            
            ---
            
            ## foo.xml_id.upper
            
            File: config/services.xml
            9:         <service id="foo.xml_id.UPPER" class="My\Foo\Service\Targets"/>
            
            ---
            
            ## foo.yml_id
            
            File: config/services.yml
            5:     foo.yml_id:
            6:         class: My\Foo\Service\Targets
            7:         parent: foo.yml_id.parent
            """.trimIndent(),
            byClassName.trim()
        )
        assertUsesRealLineBreaks(byClassName)
    }

    fun testCollectResolvesResourcePrototypeForAutowiredClass() {
        myFixture.copyFileToProject("stubs/fixtures/ResourceFooService.php", "Service/ResourceFooService.php")
        myFixture.copyFileToProject("stubs/fixtures/resource_based_services.yml", "config/resource_services.yml")

        val output = SymfonyServiceLocatorCollector(project).collect("\\App\\Service\\ResourceFooService")

        assertEquals(
            """
            ## App\Service\ResourceFooService
            
            [AUTOWIRED] Auto-registered via resource/prototype definition.
            
            File: config/resource_services.yml
            6:     App\Service\:
            7:         resource: '../Service/*'
            """.trimIndent(),
            output.trim()
        )
    }

    fun testCollectReturnsNotFoundMessage() {
        assertEquals(
            "No service found for: missing.service",
            SymfonyServiceLocatorCollector(project).collect("missing.service")
        )
    }

    fun testCollectResolvesPhpFluentServiceDefinition() {
        myFixture.copyFileToProject("stubs/fixtures/services.php", "config/services_fluent.php")

        val output = SymfonyServiceLocatorCollector(project).collect("php_twig.command.debug")

        assertEquals(
            """
            ## php_twig.command.debug
            
            File: config/services_fluent.php
            8:     ${'$'}container->services()
            9:         ->set('php_twig.command.debug', PhpTargets::class)
            """.trimIndent(),
            output.trim()
        )
    }

    fun testCollectResolvesPhpArrayServiceDefinition() {
        myFixture.addFileToProject(
            "config/services_array_min.php",
            """
            <?php
            
            namespace Symfony\Component\DependencyInjection\Loader\Configurator;
            
            use My\Foo\Service\PhpArrayTargets;
            
            return App::config([
                'services' => [
                    'php_array.before' => null,
                    'php_array.service' => [
                        'class' => PhpArrayTargets::class,
                    ],
                    'php_array.after' => null,
                    'php_array.after_two' => null,
                ],
            ]);
            """.trimIndent()
        )

        val output = SymfonyServiceLocatorCollector(project).collect("php_array.service")

        assertEquals(
            """
            ## php_array.service
            
            File: config/services_array_min.php
            10:         'php_array.service' => [
            11:             'class' => PhpArrayTargets::class,
            12:         ],
            """.trimIndent(),
            output.trim()
        )
    }
}
