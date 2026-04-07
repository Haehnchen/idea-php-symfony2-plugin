package fr.adrienbrault.idea.symfony2plugin.tests.asset;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.VfsTestUtil;
import fr.adrienbrault.idea.symfony2plugin.asset.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetDirectoryReaderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testSimpleFileResolving() {
        VirtualFile webDirectory = VfsTestUtil.createDir(getProject().getBaseDir(), "web");
        VfsTestUtil.createFile(webDirectory, "test.js");
        VirtualFile fooDirectory = VfsTestUtil.createDir(webDirectory, "foobar/foo");
        VfsTestUtil.createFile(fooDirectory, "foobar.js");

        assertNotNull(new AssetDirectoryReader()
            .resolveAssetFile(getProject(), "test.js").stream()
            .filter(virtualFile -> "test.js".equals(virtualFile.getName()))
            .findFirst()
            .orElse(null)
        );

        assertNotNull(new AssetDirectoryReader()
            .resolveAssetFile(getProject(), "foobar///foo\\/foobar.js").stream()
            .filter(virtualFile -> "foobar.js".equals(virtualFile.getName()))
            .findFirst()
            .orElse(null)
        );

        assertNotNull(new AssetDirectoryReader()
            .resolveAssetFile(getProject(), "foobar///foo\\/*").stream()
            .filter(virtualFile -> virtualFile.isDirectory() && "foo".equals(virtualFile.getName()))
            .findFirst()
            .orElse(null)
        );

        assertNotNull(new AssetDirectoryReader()
            .resolveAssetFile(getProject(), "foobar///foo\\/*.js").stream()
            .filter(virtualFile -> virtualFile.isDirectory() && "foo".equals(virtualFile.getName()))
            .findFirst()
            .orElse(null)
        );
    }
}
