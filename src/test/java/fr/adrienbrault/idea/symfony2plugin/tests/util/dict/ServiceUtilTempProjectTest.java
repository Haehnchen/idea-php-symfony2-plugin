package fr.adrienbrault.idea.symfony2plugin.tests.util.dict;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceResourceGlobMatcher;
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

        assertTrue(ServiceResourceGlobMatcher.create(file, Collections.singleton("../src/*"), Collections.emptyList()).matches(file2));
        assertTrue(ServiceResourceGlobMatcher.create(file, Collections.singleton("../src/Foobar/Bar.php"), Collections.emptyList()).matches(file2));

        assertTrue(ServiceResourceGlobMatcher.create(file, Collections.singleton("../src/{Foobar,Entity,Migrations,Tests,Kernel.php}"), Collections.emptyList()).matches(file2));
        assertFalse(ServiceResourceGlobMatcher.create(file, Collections.singleton("../src/{Entity,Migrations,Tests,Kernel.php}"), Collections.emptyList()).matches(file2));

        // exclude
        assertFalse(ServiceResourceGlobMatcher.create(file, Collections.singleton("../src/*"), Collections.singleton("../src/*")).matches(file2));
        assertFalse(ServiceResourceGlobMatcher.create(file, Collections.singleton("../src/*"), Collections.singleton("../src/{Foobar,Kernel.php}")).matches(file2));
        assertTrue(ServiceResourceGlobMatcher.create(file, Collections.singleton("../src/*"), Collections.singleton("../src/{Migrations,Kernel.php}")).matches(file2));

        // nested: not supported and must not break in exception
        assertFalse(ServiceResourceGlobMatcher.create(file, Collections.singleton("../src/{DependencyInjection,Entity,Migrations,Tests,Kernel.php,Service/{IspConfiguration,DataCollection}}"), Collections.emptyList()).matches(file2));
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

        assertTrue(ServiceResourceGlobMatcher.create(file, Collections.singleton("../../*"), Collections.emptyList()).matches(file2));
        assertFalse(ServiceResourceGlobMatcher.create(file, Collections.singleton("../../*"), Collections.singleton("../../Bar")).matches(file2));
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

        assertFalse(ServiceResourceGlobMatcher.create(file, Collections.singleton(""), Collections.singleton("")).matches(file2));
        assertFalse(ServiceResourceGlobMatcher.create(file, Collections.singleton("src"), Collections.singleton("src")).matches(file2));
        assertFalse(ServiceResourceGlobMatcher.create(file, Collections.singleton("/aaa"), Collections.singleton("/aaaa")).matches(file2));
        assertFalse(ServiceResourceGlobMatcher.create(file, Collections.singleton("/"), Collections.singleton("/")).matches(file2));
        assertFalse(ServiceResourceGlobMatcher.create(file, Collections.singleton("a"), Collections.singleton("/")).matches(file2));
        assertFalse(ServiceResourceGlobMatcher.create(file, Collections.singleton(""), Collections.emptyList()).matches(file2));
        assertFalse(ServiceResourceGlobMatcher.create(file, Collections.singleton("src"), Collections.emptyList()).matches(file2));
        assertFalse(ServiceResourceGlobMatcher.create(file, Collections.singleton("/"), Collections.emptyList()).matches(file2));
        assertFalse(ServiceResourceGlobMatcher.create(file, Collections.singleton(".."), Collections.singleton("..")).matches(file2));
    }

    public void testServiceResourcesWithArrays() {
        VirtualFile file = createFile("config/services.yml");

        VirtualFile file2 = createFile(
            "src/Foobar/Bar.php",
            "<?php\n"
        );

        assertTrue(ServiceResourceGlobMatcher.create(file, Collections.singleton("../src/*"), Collections.emptyList()).matches(file2));
        assertTrue(ServiceResourceGlobMatcher.create(file, Arrays.asList("../src2/Foobar/Bar.php", "../src/Foobar/Bar.php"), Collections.emptyList()).matches(file2));

        assertTrue(ServiceResourceGlobMatcher.create(file, Arrays.asList("../src/{Entity,Tests,Kernel.php}", "../src/{Foobar,EntityKernel.php}"), Collections.emptyList()).matches(file2));
        assertFalse(ServiceResourceGlobMatcher.create(file, Collections.singleton("../src/{Entity,Migrations,Tests,Kernel.php}"), Collections.emptyList()).matches(file2));

        // exclude
        assertFalse(ServiceResourceGlobMatcher.create(file, Arrays.asList("../foobar/*", "../src/*"), Arrays.asList("../foo/*", "../src/*")).matches(file2));
        assertFalse(ServiceResourceGlobMatcher.create(file, Collections.singleton("../src/*"), Arrays.asList("../src/{Bar,Kernel.php}", "../src/{Foobar,Kernel.php}")).matches(file2));
        assertTrue(ServiceResourceGlobMatcher.create(file, Collections.singleton("../src/*"), Arrays.asList("../src/{Migrations,Kernel.php}", "../src/{Migrations2,Kernel.php}")).matches(file2));
    }

    public void testCompiledServiceResourcesMatcher() {
        VirtualFile file = createFile("config/services.yml");

        VirtualFile file2 = createFile(
            "src/Foobar/Bar.php",
            "<?php\n"
        );

        ServiceResourceGlobMatcher matcher = ServiceResourceGlobMatcher.create(
            file,
            Collections.singleton("../src/*"),
            Collections.singleton("../src/{Foobar,Kernel.php}")
        );

        assertFalse(matcher.matches(file2));
    }

    public void testCompiledServiceResourcesMatcherWithExcludeMiss() {
        VirtualFile file = createFile("config/services.yml");

        VirtualFile file2 = createFile(
            "src/Foobar/Bar.php",
            "<?php\n"
        );

        ServiceResourceGlobMatcher matcher = ServiceResourceGlobMatcher.create(
            file,
            Collections.singleton("../src/*"),
            Collections.singleton("../src/{Migrations,Kernel.php}")
        );

        assertTrue(matcher.matches(file2));
    }
}
