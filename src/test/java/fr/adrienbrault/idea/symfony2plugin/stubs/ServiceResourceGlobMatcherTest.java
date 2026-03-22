package fr.adrienbrault.idea.symfony2plugin.tests.stubs;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceResourceGlobMatcher;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.List;

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
}
