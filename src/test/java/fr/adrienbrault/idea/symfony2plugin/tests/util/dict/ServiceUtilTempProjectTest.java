package fr.adrienbrault.idea.symfony2plugin.tests.util.dict;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceUtilTempProjectTest extends SymfonyTempCodeInsightFixtureTestCase {
    public void testServiceResources() {
        VirtualFile file = createFile(
            "config/services.yml",
                "services:\n" +
                "    App\\:\n" +
                "        resource: '../src/*'\n" +
                "        exclude: '../src/{DependencyInjection,Entity,Migrations,Tests,Kernel.php}'"
        );

        VirtualFile file2 = createFile(
            "src/Foobar/Bar.php",
            "<?php\n" +
                "namespace App\\Foobar;\n" +
                "class Bar {}\n"
        );

        assertTrue(ServiceIndexUtil.matchesResourcesGlob(file, file2, "../src/*", null));
        assertTrue(ServiceIndexUtil.matchesResourcesGlob(file, file2, "../src/Foobar/Bar.php", null));

        assertTrue(ServiceIndexUtil.matchesResourcesGlob(file, file2, "../src/{Foobar,Entity,Migrations,Tests,Kernel.php}", null));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, "../src/{Entity,Migrations,Tests,Kernel.php}", null));

        // exclude
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, "../src/*", "../src/*"));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, "../src/*", "../src/{Foobar,Kernel.php}"));
        assertTrue(ServiceIndexUtil.matchesResourcesGlob(file, file2, "../src/*", "../src/{Migrations,Kernel.php}"));

        // nested: not supported and must not break in exception
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, "../src/{DependencyInjection,Entity,Migrations,Tests,Kernel.php,Service/{IspConfiguration,DataCollection}}", null));
    }

    public void testServiceResourcesWithDepth() {
        VirtualFile file = createFile(
            "src/Foo/Resources/config/services.yml",
            "services:\n" +
                "    App\\:\n" +
                "        resource: '../../*'\n" +
                "        exclude: '../../src/{DependencyInjection,Entity,Migrations,Tests,Kernel.php}'"
        );

        VirtualFile file2 = createFile(
            "src/Foo/Bar/Bar.php",
            "<?php\n" +
                "namespace App\\Foobar;\n" +
                "class Bar {}\n"
        );

        assertTrue(ServiceIndexUtil.matchesResourcesGlob(file, file2, "../../*", null));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, "../../*", "../../Bar"));
    }

    public void testPossibleResourceCondition() {
        VirtualFile file = createFile(
            "src/Foo/Resources/config/services.yml",
            "services:\n" +
                "    App\\:\n" +
                "        resource: ''\n"
        );

        VirtualFile file2 = createFile(
            "src/Foo/Bar/Bar.php",
            "<?php\n" +
                "namespace App\\Foobar;\n" +
                "class Bar {}\n"
        );

        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, "", ""));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, "src", "src"));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, "/aaa", "/aaaa"));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, "/", "/"));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, "a", "/"));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, "", null));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, "src", null));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, "/", null));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, "..", ".."));
    }
}
