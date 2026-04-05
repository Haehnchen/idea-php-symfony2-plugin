package fr.adrienbrault.idea.symfony2plugin.tests.dic.container.util;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil
 */
public class ServiceContainerUtilResourceTest extends SymfonyLightCodeInsightFixtureTestCase {

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/container/util/fixtures";
    }

    public void testResolveBaseDirectoryFromResourcePatternSimple() {
        myFixture.copyFileToProject("resource_services.yaml", "config/services.yaml");
        myFixture.copyFileToProject("resource_FooService.php", "src/Service/FooService.php");

        VirtualFile configFile = myFixture.findFileInTempDir("config/services.yaml");
        assertNotNull(configFile);

        // Pattern: "../src/" → resolves to src/
        VirtualFile dir = ServiceContainerUtil.resolveBaseDirectoryFromResourcePattern("../src/", configFile);
        assertNotNull(dir);
        assertEquals("src", dir.getName());
    }

    public void testResolveBaseDirectoryFromResourcePatternWithGlobStar() {
        myFixture.copyFileToProject("resource_services.yaml", "config/services.yaml");
        myFixture.copyFileToProject("resource_FooService.php", "src/Service/FooService.php");

        VirtualFile configFile = myFixture.findFileInTempDir("config/services.yaml");
        assertNotNull(configFile);

        // Pattern: "../src/*" → resolves to src/
        VirtualFile dir = ServiceContainerUtil.resolveBaseDirectoryFromResourcePattern("../src/*", configFile);
        assertNotNull(dir);
        assertEquals("src", dir.getName());
    }

    public void testResolveBaseDirectoryFromResourcePatternWithBraceGlob() {
        myFixture.copyFileToProject("resource_services.yaml", "config/services.yaml");
        myFixture.copyFileToProject("resource_FooService.php", "src/Service/FooService.php");

        VirtualFile configFile = myFixture.findFileInTempDir("config/services.yaml");
        assertNotNull(configFile);

        // Pattern: "../src/{Service,Controller}/*" → resolves to src/
        VirtualFile dir = ServiceContainerUtil.resolveBaseDirectoryFromResourcePattern("../src/{Service,Controller}/*", configFile);
        assertNotNull(dir);
        assertEquals("src", dir.getName());
    }

    public void testResolveBaseDirectoryFromResourcePatternNonExistentReturnsNull() {
        myFixture.copyFileToProject("resource_services.yaml", "config/services.yaml");

        VirtualFile configFile = myFixture.findFileInTempDir("config/services.yaml");
        assertNotNull(configFile);

        // Pattern pointing to non-existent directory
        VirtualFile dir = ServiceContainerUtil.resolveBaseDirectoryFromResourcePattern("../nonexistent/", configFile);
        assertNull(dir);
    }

    public void testGetPhpClassFromResourcesOptimizedReturnsMatchingClass() {
        myFixture.copyFileToProject("resource_services.yaml", "config/services.yaml");
        myFixture.copyFileToProject("resource_FooService.php", "src/Service/FooService.php");

        VirtualFile configFile = myFixture.findFileInTempDir("config/services.yaml");
        assertNotNull(configFile);

        Collection<String> classes = ServiceContainerUtil.getPhpClassFromResources(
            getProject(),
            "App\\Service\\",
            configFile,
            List.of("../src/Service/*"),
            Collections.emptyList()
        );

        assertTrue("Expected FooService to be resolved", classes.contains("\\App\\Service\\FooService"));
    }

    public void testGetPhpClassFromResourcesWithExactFileResource() {
        myFixture.copyFileToProject("resource_services.yaml", "config/services.yaml");
        myFixture.copyFileToProject("resource_FooService.php", "src/Service/FooService.php");

        VirtualFile configFile = myFixture.findFileInTempDir("config/services.yaml");
        assertNotNull(configFile);

        Collection<String> classes = ServiceContainerUtil.getPhpClassFromResources(
            getProject(),
            "App\\Service\\",
            configFile,
            List.of("../src/Service/FooService.php"),
            Collections.emptyList()
        );

        assertTrue("Expected FooService to be resolved for exact file resource", classes.contains("\\App\\Service\\FooService"));
    }

    public void testGetPhpClassFromResourcesOptimizedExcludesAbstractClass() {
        myFixture.copyFileToProject("resource_services.yaml", "config/services.yaml");
        myFixture.copyFileToProject("resource_FooService.php", "src/Service/FooService.php");
        myFixture.copyFileToProject("resource_AbstractService.php", "src/Service/AbstractService.php");

        VirtualFile configFile = myFixture.findFileInTempDir("config/services.yaml");
        assertNotNull(configFile);

        Collection<String> classes = ServiceContainerUtil.getPhpClassFromResources(
            getProject(),
            "App\\Service\\",
            configFile,
            List.of("../src/Service/*"),
            Collections.emptyList()
        );

        assertFalse("AbstractService must not be included", classes.contains("\\App\\Service\\AbstractService"));
        assertTrue("FooService must be included", classes.contains("\\App\\Service\\FooService"));
    }

    public void testGetPhpClassFromResourcesWithExcludePattern() {
        myFixture.copyFileToProject("resource_services.yaml", "config/services.yaml");
        myFixture.copyFileToProject("resource_FooService.php", "src/Service/FooService.php");

        VirtualFile configFile = myFixture.findFileInTempDir("config/services.yaml");
        assertNotNull(configFile);

        Collection<String> classes = ServiceContainerUtil.getPhpClassFromResources(
            getProject(),
            "App\\Service\\",
            configFile,
            List.of("../src/Service/*"),
            List.of("../src/Service/FooService.php")
        );

        assertFalse("FooService must be excluded", classes.contains("\\App\\Service\\FooService"));
    }

    public void testGetPhpClassFromResourcesOptimizedWithMultipleResources() {
        myFixture.copyFileToProject("resource_services.yaml", "config/services.yaml");
        myFixture.copyFileToProject("resource_FooService.php", "src/Service/FooService.php");
        myFixture.copyFileToProject("resource_BarController.php", "src/Controller/BarController.php");

        VirtualFile configFile = myFixture.findFileInTempDir("config/services.yaml");
        assertNotNull(configFile);

        // Two resources: Service/* and Controller/*
        Collection<String> serviceClasses = ServiceContainerUtil.getPhpClassFromResources(
            getProject(),
            "App\\Service\\",
            configFile,
            List.of("../src/Service/*", "../src/Controller/*"),
            Collections.emptyList()
        );
        assertTrue("FooService must be included via first resource", serviceClasses.contains("\\App\\Service\\FooService"));

        Collection<String> controllerClasses = ServiceContainerUtil.getPhpClassFromResources(
            getProject(),
            "App\\Controller\\",
            configFile,
            List.of("../src/Service/*", "../src/Controller/*"),
            Collections.emptyList()
        );
        assertTrue("BarController must be included via second resource", controllerClasses.contains("\\App\\Controller\\BarController"));
    }

    public void testGetPhpClassFromResourcesWithMultipleExcludes() {
        myFixture.copyFileToProject("resource_services.yaml", "config/services.yaml");
        myFixture.copyFileToProject("resource_FooService.php", "src/Service/FooService.php");
        myFixture.copyFileToProject("resource_AbstractService.php", "src/Service/AbstractService.php");
        myFixture.copyFileToProject("resource_BarController.php", "src/Controller/BarController.php");

        VirtualFile configFile = myFixture.findFileInTempDir("config/services.yaml");
        assertNotNull(configFile);

        Collection<String> classes = ServiceContainerUtil.getPhpClassFromResources(
            getProject(),
            "App\\Service\\",
            configFile,
            List.of("../src/Service/*"),
            List.of("../src/Service/FooService.php", "../src/Service/AbstractService.php")
        );

        assertFalse("FooService must be excluded", classes.contains("\\App\\Service\\FooService"));
        assertFalse("AbstractService must be excluded", classes.contains("App\\Service\\AbstractService"));
    }

    public void testGetPhpClassFromResourcesNonMatchingNamespaceReturnsEmpty() {
        myFixture.copyFileToProject("resource_services.yaml", "config/services.yaml");
        myFixture.copyFileToProject("resource_FooService.php", "src/Service/FooService.php");

        VirtualFile configFile = myFixture.findFileInTempDir("config/services.yaml");
        assertNotNull(configFile);

        Collection<String> classes = ServiceContainerUtil.getPhpClassFromResources(
            getProject(),
            "Other\\Namespace\\",
            configFile,
            List.of("../src/Service/*"),
            Collections.emptyList()
        );

        assertTrue("No classes should match a non-existing namespace", classes.isEmpty());
    }

    public void testGetPhpClassFromResourcesWithPhpConfigFile() {
        myFixture.addFileToProject("config/services.php", "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "return App::config([\n" +
            "    'services' => [\n" +
            "        'App\\\\Service\\\\' => [\n" +
            "            'resource' => '../src/Service/*',\n" +
            "        ],\n" +
            "    ],\n" +
            "]);");
        myFixture.addFileToProject("src/Service/FooService.php", "<?php\n" +
            "namespace App\\Service;\n" +
            "class FooService {}\n");

        VirtualFile configFile = myFixture.findFileInTempDir("config/services.php");
        assertNotNull(configFile);

        Collection<String> classes = ServiceContainerUtil.getPhpClassFromResources(
            getProject(),
            "App\\Service\\",
            configFile,
            List.of("../src/Service/*"),
            Collections.emptyList()
        );

        assertContainsElements(classes, "\\App\\Service\\FooService");
    }

    public void testGetPhpClassFromResourcesWithPhpConfigRootNamespaceDirectory() {
        myFixture.addFileToProject("config/services.php", "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "return App::config([\n" +
            "    'services' => [\n" +
            "        'App\\\\' => [\n" +
            "            'resource' => '../src/',\n" +
            "            'exclude' => [\n" +
            "                '../src/DependencyInjection/',\n" +
            "                '../src/Entity/',\n" +
            "                '../src/Kernel.php',\n" +
            "            ],\n" +
            "        ],\n" +
            "    ],\n" +
            "]);");
        myFixture.addFileToProject("src/ResourceLineMarkerFoo.php", "<?php\n" +
            "namespace App;\n" +
            "class ResourceLineMarkerFoo {}\n");
        myFixture.addFileToProject("src/DependencyInjection/ShouldBeExcluded.php", "<?php\n" +
            "namespace App\\DependencyInjection;\n" +
            "class ShouldBeExcluded {}\n");

        VirtualFile configFile = myFixture.findFileInTempDir("config/services.php");
        assertNotNull(configFile);

        Collection<String> classes = ServiceContainerUtil.getPhpClassFromResources(
            getProject(),
            "App\\",
            configFile,
            List.of("../src/"),
            List.of("../src/DependencyInjection/", "../src/Entity/", "../src/Kernel.php")
        );

        assertContainsElements(classes, "\\App\\ResourceLineMarkerFoo");
        assertDoesntContain(classes, "\\App\\DependencyInjection\\ShouldBeExcluded");
    }

}
