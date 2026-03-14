package fr.adrienbrault.idea.symfony2plugin.tests.config.xml;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.VfsTestUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.config.xml.XmlCompletionContributor
 */
public class XmlCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/xml/fixtures";
    }

    public void testBundleImportResourceCompletionWithLegacyConfigStructure() {
        VirtualFile bundleFile = myFixture.copyFileToProject("FooBundle.php");
        VirtualFile resourcesDir = VfsTestUtil.createDir(bundleFile.getParent(), "Resources");
        VirtualFile configDir = VfsTestUtil.createDir(resourcesDir, "config");
        VfsTestUtil.createFile(configDir, "routing.yml");

        assertCompletionContains(XmlFileType.INSTANCE,
            "<?xml version=\"1.0\"?>\n" +
            "<container>\n" +
            "    <imports>\n" +
            "        <import resource=\"<caret>\"/>\n" +
            "    </imports>\n" +
            "</container>",
            "FooBundle/Resources/config/routing.yml"
        );
    }

    public void testBundleImportResourceCompletionWithNewConfigStructure() {
        VirtualFile bundleFile = myFixture.copyFileToProject("FooBundle.php");
        VirtualFile configDir = VfsTestUtil.createDir(bundleFile.getParent(), "config");
        VfsTestUtil.createFile(configDir, "routing.yml");

        assertCompletionContains(XmlFileType.INSTANCE,
            "<?xml version=\"1.0\"?>\n" +
            "<container>\n" +
            "    <imports>\n" +
            "        <import resource=\"<caret>\"/>\n" +
            "    </imports>\n" +
            "</container>",
            "FooBundle/config/routing.yml"
        );
    }
}
