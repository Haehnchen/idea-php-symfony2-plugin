package fr.adrienbrault.idea.symfony2plugin.tests.asset;

import fr.adrienbrault.idea.symfony2plugin.asset.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetDirectoryReaderTest extends SymfonyTempCodeInsightFixtureTestCase {
    public void testSimpleFileResolving() {
        createFile("web/test.js");
        createFile("web/foobar/foo/foobar.js");

        assertNotNull(new AssetDirectoryReader()
            .resolveAssetFile(getProject(), "test.js").stream()
            .filter(virtualFile -> "test.js".equals(virtualFile.getName()))
            .findFirst()
            .orElseGet(null)
        );

        assertNotNull(new AssetDirectoryReader()
            .resolveAssetFile(getProject(), "foobar///foo\\/foobar.js").stream()
            .filter(virtualFile -> "foobar.js".equals(virtualFile.getName()))
            .findFirst()
            .orElseGet(null)
        );

        assertNotNull(new AssetDirectoryReader()
            .resolveAssetFile(getProject(), "foobar///foo\\/*").stream()
            .filter(virtualFile -> virtualFile.isDirectory() && "foo".equals(virtualFile.getName()))
            .findFirst()
            .orElseGet(null)
        );

        assertNotNull(new AssetDirectoryReader()
            .resolveAssetFile(getProject(), "foobar///foo\\/*.js").stream()
            .filter(virtualFile -> virtualFile.isDirectory() && "foo".equals(virtualFile.getName()))
            .findFirst()
            .orElseGet(null)
        );
    }
}
