package fr.adrienbrault.idea.symfony2plugin.tests.templating.path;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.path.BundleTwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.path.BundleTwigNamespaceExtension
 */
public class BundleTwigNamespaceExtensionTest extends SymfonyLightCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        VirtualFile virtualFile = myFixture.copyFileToProject("classes.php");
        VfsTestUtil.createDir(virtualFile.getParent(), "Resources/views");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testThatBundleNamespacesAreAdded() {
        Collection<TwigPath> namespaces = new BundleTwigNamespaceExtension()
            .getNamespaces(new TwigNamespaceExtensionParameter(getProject()));

        assertNotNull(ContainerUtil.find(namespaces, twigPath ->
            "FooBundle".equals(twigPath.getNamespace()) && "/src/Resources/views".equals(twigPath.getPath()))
        );

        assertNotNull(ContainerUtil.find(namespaces, twigPath ->
            "Foo".equals(twigPath.getNamespace()) && "/src/Resources/views".equals(twigPath.getPath()))
        );
    }
}
