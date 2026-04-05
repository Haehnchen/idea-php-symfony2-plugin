package fr.adrienbrault.idea.symfony2plugin.stubs;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ServiceResourceGlobMatcher
 */
public class ServiceResourceGlobMatcherTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testMatchesFileInsideResourceDirectory() {
        VirtualFile servicesFile = myFixture.addFileToProject("config/services.yaml", "").getVirtualFile();
        VirtualFile phpFile = myFixture.addFileToProject("src/Service/Foo.php", "<?php").getVirtualFile();

        assertTrue(ServiceResourceGlobMatcher.create(servicesFile, List.of("../src/"), List.of()).matches(phpFile));
    }

    public void testDoesNotMatchFileOutsideResourceDirectory() {
        VirtualFile servicesFile = myFixture.addFileToProject("config/services.yaml", "").getVirtualFile();
        VirtualFile phpFile = myFixture.addFileToProject("lib/Other.php", "<?php").getVirtualFile();

        assertFalse(ServiceResourceGlobMatcher.create(servicesFile, List.of("../src/"), List.of()).matches(phpFile));
    }

    public void testExcludedFileIsNotMatched() {
        VirtualFile servicesFile = myFixture.addFileToProject("config/services2.yaml", "").getVirtualFile();
        VirtualFile phpFile = myFixture.addFileToProject("src/Entity/Bar.php", "<?php").getVirtualFile();

        assertFalse(ServiceResourceGlobMatcher.create(
            servicesFile,
            List.of("../src/"),
            List.of("../src/Entity/")
        ).matches(phpFile));
    }

    public void testMatchesWithNoResourcesReturnsFalse() {
        VirtualFile servicesFile = myFixture.addFileToProject("config/services3.yaml", "").getVirtualFile();
        VirtualFile phpFile = myFixture.addFileToProject("src/Foo.php", "<?php").getVirtualFile();

        assertFalse(ServiceResourceGlobMatcher.create(servicesFile, List.of(), List.of()).matches(phpFile));
    }

    public void testMatchesBracePattern() {
        VirtualFile servicesFile = myFixture.addFileToProject("config/services.yaml", "").getVirtualFile();
        VirtualFile phpFile = myFixture.addFileToProject("src/Foobar/Bar.php", "<?php").getVirtualFile();

        ServiceResourceGlobMatcher matcher = ServiceResourceGlobMatcher.create(
            servicesFile,
            List.of("../src/{Foobar,EntityKernel.php}"),
            List.of("../src/{Migrations,Kernel.php}")
        );

        assertTrue(matcher.matches(phpFile));
    }

    public void testMatchesRecursiveDirectory() {
        VirtualFile servicesFile = myFixture.addFileToProject("src/Foo/Resources/config/services.yml", "").getVirtualFile();
        VirtualFile phpFile = myFixture.addFileToProject("src/Foo/Bar/Bar.php", "<?php").getVirtualFile();

        assertTrue(ServiceResourceGlobMatcher.create(servicesFile, List.of("../../*"), List.of()).matches(phpFile));
        assertFalse(ServiceResourceGlobMatcher.create(servicesFile, List.of("../../*"), List.of("../../Bar")).matches(phpFile));
    }

    public void testMatchesParentDirectory() {
        VirtualFile servicesFile = myFixture.addFileToProject("src/Foo/Resources/config/services.yml", "").getVirtualFile();
        VirtualFile phpFile = myFixture.addFileToProject("src/Foo/Bar/Bar.php", "<?php").getVirtualFile();

        assertTrue(ServiceResourceGlobMatcher.create(servicesFile, List.of("../.."), List.of()).matches(phpFile));
        assertFalse(ServiceResourceGlobMatcher.create(servicesFile, List.of("../.."), List.of("../../Bar")).matches(phpFile));
    }

    public void testMatchesScopedDirectoryWithExcludes() {
        VirtualFile servicesFile = myFixture.addFileToProject("src/App/Resources/config/services.yml", "").getVirtualFile();
        VirtualFile allowedFile = myFixture.addFileToProject("src/App/Api/FooService.php", "<?php").getVirtualFile();
        VirtualFile excludedFile = myFixture.addFileToProject("src/App/Api/Exception/BarException.php", "<?php").getVirtualFile();

        ServiceResourceGlobMatcher matcher = ServiceResourceGlobMatcher.create(
            servicesFile,
            List.of("../../Api/"),
            Arrays.asList("../../Api/Exception/", "../../Api/Model/")
        );

        assertTrue(matcher.matches(allowedFile));
        assertFalse(matcher.matches(excludedFile));
    }

    public void testMatchesParentDirectoryWithBraceExcludeGroup() {
        VirtualFile servicesFile = myFixture.addFileToProject("src/ModuleAlpha/Resources/config/services.yml", "").getVirtualFile();
        VirtualFile controllerFile = myFixture.addFileToProject("src/ModuleAlpha/Controller/FooController.php", "<?php").getVirtualFile();
        VirtualFile excludedDirectoryFile = myFixture.addFileToProject("src/ModuleAlpha/Transfer/FooData.php", "<?php").getVirtualFile();

        ServiceResourceGlobMatcher matcher = ServiceResourceGlobMatcher.create(
            servicesFile,
            List.of("../.."),
            List.of("../../{DependencyInjection,Transfer,Entity,Exception,Tests,Kernel.php}")
        );

        assertTrue(matcher.matches(controllerFile));
        assertFalse(matcher.matches(excludedDirectoryFile));
    }

    public void testMatchesWildcardDirectoryWithBraceExcludeGroup() {
        VirtualFile servicesFile = myFixture.addFileToProject("src/ModuleBeta/Resources/config/services.yml", "").getVirtualFile();
        VirtualFile serviceFile = myFixture.addFileToProject("src/ModuleBeta/Service/FooService.php", "<?php").getVirtualFile();
        VirtualFile excludedDirectoryFile = myFixture.addFileToProject("src/ModuleBeta/Transfer/FooData.php", "<?php").getVirtualFile();

        ServiceResourceGlobMatcher matcher = ServiceResourceGlobMatcher.create(
            servicesFile,
            List.of("../../*"),
            List.of("../../{Transfer}")
        );

        assertTrue(matcher.matches(serviceFile));
        assertFalse(matcher.matches(excludedDirectoryFile));
    }

    public void testMatchesWildcardDirectoryWithRecursiveFilenameExclude() {
        VirtualFile servicesFile = myFixture.addFileToProject("src/ModuleGamma/Resources/config/services.yml", "").getVirtualFile();
        VirtualFile regularFile = myFixture.addFileToProject("src/ModuleGamma/Entry/FooEntry.php", "<?php").getVirtualFile();
        VirtualFile excludedFileOne = myFixture.addFileToProject("src/ModuleGamma/SegmentOne/ExcludedMarker.php", "<?php").getVirtualFile();
        VirtualFile excludedFileTwo = myFixture.addFileToProject("src/ModuleGamma/SegmentTwo/ExcludedPayload.php", "<?php").getVirtualFile();

        ServiceResourceGlobMatcher matcher = ServiceResourceGlobMatcher.create(
            servicesFile,
            List.of("../../*"),
            List.of("../../**/{ExcludedMarker.php,ExcludedPayload.php}")
        );

        assertTrue(matcher.matches(regularFile));
        assertFalse(matcher.matches(excludedFileOne));
        assertFalse(matcher.matches(excludedFileTwo));
    }

    public void testCompilesExpectedRegexPatterns() throws Exception {
        VirtualFile excludeServicesFile = myFixture.addFileToProject("src/ModuleGamma/Resources/config/services.yml", "").getVirtualFile();
        ServiceResourceGlobMatcher excludeMatcher = ServiceResourceGlobMatcher.create(
            excludeServicesFile,
            List.of("../../*"),
            List.of("../../**/{ExcludedMarker.php,ExcludedPayload.php}")
        );

        assertEquals(
            "^/src/src/ModuleGamma.*$",
            getCompiledPatterns(excludeMatcher, "resourceMatchers").getFirst().pattern()
        );

        assertEquals(
            "^/src/src/ModuleGamma/.*/(?:ExcludedMarker\\.php|ExcludedPayload\\.php).*$",
            getCompiledPatterns(excludeMatcher, "excludeMatchers").getFirst().pattern()
        );

        VirtualFile parentServicesFile = myFixture.addFileToProject("src/ModuleAlpha/Resources/config/services.yml", "").getVirtualFile();
        ServiceResourceGlobMatcher parentMatcher = ServiceResourceGlobMatcher.create(
            parentServicesFile,
            List.of("../.."),
            List.of("../../{DependencyInjection,Transfer,Entity,Exception,Tests,Kernel.php}")
        );

        assertEquals(
            "^/src/src/ModuleAlpha/.*$",
            getCompiledPatterns(parentMatcher, "resourceMatchers").getFirst().pattern()
        );

        assertEquals(
            "^/src/src/ModuleAlpha/(?:DependencyInjection|Transfer|Entity|Exception|Tests|Kernel\\.php).*$",
            getCompiledPatterns(parentMatcher, "excludeMatchers").getFirst().pattern()
        );

        VirtualFile scopedServicesFile = myFixture.addFileToProject("src/App/Resources/config/services.yml", "").getVirtualFile();
        ServiceResourceGlobMatcher scopedMatcher = ServiceResourceGlobMatcher.create(
            scopedServicesFile,
            List.of("../../Api/"),
            List.of("../../Api/Exception/", "../../Api/Model/")
        );

        assertEquals(
            "^/src/src/App/Api.*$",
            getCompiledPatterns(scopedMatcher, "resourceMatchers").getFirst().pattern()
        );

        assertEquals(
            "^/src/src/App/Api/Exception.*$",
            getCompiledPatterns(scopedMatcher, "excludeMatchers").getFirst().pattern()
        );

        assertEquals(
            "^/src/src/App/Api/Model.*$",
            getCompiledPatterns(scopedMatcher, "excludeMatchers").get(1).pattern()
        );
    }

    public void testMatchesArrayPatterns() {
        VirtualFile servicesFile = myFixture.addFileToProject("config/services-array.yaml", "").getVirtualFile();
        VirtualFile phpFile = myFixture.addFileToProject("src/Foobar/Bar.php", "<?php").getVirtualFile();

        assertTrue(ServiceResourceGlobMatcher.create(
            servicesFile,
            Arrays.asList("../src2/Foobar/Bar.php", "../src/Foobar/Bar.php"),
            Collections.emptyList()
        ).matches(phpFile));

        assertTrue(ServiceResourceGlobMatcher.create(
            servicesFile,
            Arrays.asList("../src/{Entity,Tests,Kernel.php}", "../src/{Foobar,EntityKernel.php}"),
            Collections.emptyList()
        ).matches(phpFile));

        assertFalse(ServiceResourceGlobMatcher.create(
            servicesFile,
            Arrays.asList("../foobar/*", "../src/*"),
            Arrays.asList("../foo/*", "../src/*")
        ).matches(phpFile));
    }

    public void testMatchesInvalidPatternsAsFalse() {
        VirtualFile servicesFile = myFixture.addFileToProject("src/Foo/Resources/config/services-invalid.yml", "").getVirtualFile();
        VirtualFile phpFile = myFixture.addFileToProject("src/Foo/Bar/Bar.php", "<?php").getVirtualFile();

        assertFalse(ServiceResourceGlobMatcher.create(servicesFile, Collections.singleton(""), Collections.singleton("")).matches(phpFile));
        assertFalse(ServiceResourceGlobMatcher.create(servicesFile, Collections.singleton("src"), Collections.singleton("src")).matches(phpFile));
        assertFalse(ServiceResourceGlobMatcher.create(servicesFile, Collections.singleton("/aaa"), Collections.singleton("/aaaa")).matches(phpFile));
        assertFalse(ServiceResourceGlobMatcher.create(servicesFile, Collections.singleton("/"), Collections.singleton("/")).matches(phpFile));
        assertFalse(ServiceResourceGlobMatcher.create(servicesFile, Collections.singleton("a"), Collections.singleton("/")).matches(phpFile));
        assertFalse(ServiceResourceGlobMatcher.create(servicesFile, Collections.singleton(""), Collections.emptyList()).matches(phpFile));
        assertFalse(ServiceResourceGlobMatcher.create(servicesFile, Collections.singleton("src"), Collections.emptyList()).matches(phpFile));
        assertFalse(ServiceResourceGlobMatcher.create(servicesFile, Collections.singleton("/"), Collections.emptyList()).matches(phpFile));
        assertFalse(ServiceResourceGlobMatcher.create(servicesFile, Collections.singleton(".."), Collections.singleton("..")).matches(phpFile));
    }

    @SuppressWarnings("unchecked")
    private static List<Pattern> getCompiledPatterns(ServiceResourceGlobMatcher matcher, String fieldName) throws Exception {
        Field field = ServiceResourceGlobMatcher.class.getDeclaredField(fieldName);
        field.setAccessible(true);

        return (List<Pattern>) field.get(matcher);
    }
}
