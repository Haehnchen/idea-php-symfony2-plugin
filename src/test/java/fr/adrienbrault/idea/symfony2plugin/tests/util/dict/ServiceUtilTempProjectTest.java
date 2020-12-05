package fr.adrienbrault.idea.symfony2plugin.tests.util.dict;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

import java.util.Arrays;
import java.util.Collections;

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

        assertTrue(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("../src/*"), Collections.emptyList()));
        assertTrue(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("../src/Foobar/Bar.php"), Collections.emptyList()));

        assertTrue(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("../src/{Foobar,Entity,Migrations,Tests,Kernel.php}"), Collections.emptyList()));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("../src/{Entity,Migrations,Tests,Kernel.php}"), Collections.emptyList()));

        // exclude
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("../src/*"), Collections.singleton("../src/*")));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("../src/*"), Collections.singleton("../src/{Foobar,Kernel.php}")));
        assertTrue(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("../src/*"), Collections.singleton("../src/{Migrations,Kernel.php}")));

        // nested: not supported and must not break in exception
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("../src/{DependencyInjection,Entity,Migrations,Tests,Kernel.php,Service/{IspConfiguration,DataCollection}}"), Collections.emptyList()));
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

        assertTrue(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("../../*"), Collections.emptyList()));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("../../*"), Collections.singleton("../../Bar")));
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

        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton(""), Collections.singleton("")));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("src"), Collections.singleton("src")));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("/aaa"), Collections.singleton("/aaaa")));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("/"), Collections.singleton("/")));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("a"), Collections.singleton("/")));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton(""), Collections.emptyList()));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("src"), Collections.emptyList()));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("/"), Collections.emptyList()));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton(".."), Collections.singleton("..")));
    }

    public void testServiceResourcesWithArrays() {
        VirtualFile file = createFile("config/services.yml");

        VirtualFile file2 = createFile(
            "src/Foobar/Bar.php",
            "<?php\n"
        );

        assertTrue(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("../src/*"), Collections.emptyList()));
        assertTrue(ServiceIndexUtil.matchesResourcesGlob(file, file2, Arrays.asList("../src2/Foobar/Bar.php", "../src/Foobar/Bar.php"), Collections.emptyList()));

        assertTrue(ServiceIndexUtil.matchesResourcesGlob(file, file2, Arrays.asList("../src/{Entity,Tests,Kernel.php}", "../src/{Foobar,EntityKernel.php}"), Collections.emptyList()));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("../src/{Entity,Migrations,Tests,Kernel.php}"), Collections.emptyList()));

        // exclude
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Arrays.asList("../foobar/*", "../src/*"), Arrays.asList("../foo/*", "../src/*")));
        assertFalse(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("../src/*"), Arrays.asList("../src/{Bar,Kernel.php}", "../src/{Foobar,Kernel.php}")));
        assertTrue(ServiceIndexUtil.matchesResourcesGlob(file, file2, Collections.singleton("../src/*"), Arrays.asList("../src/{Migrations,Kernel.php}", "../src/{Migrations2,Kernel.php}")));
   }
}
