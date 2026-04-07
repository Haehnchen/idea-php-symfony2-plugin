package fr.adrienbrault.idea.symfony2plugin.tests.templating.path;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.path.ContainerTwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcherKt;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.path.ContainerTwigNamespaceExtension
 */
public class ContainerTwigNamespaceExtensionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testGetNamespacesFromContainer() throws IOException {
        String containerXml = new String(Files.readAllBytes(Paths.get(
            "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/path/appDevDebugProjectContainer.xml"
        )));

        createFileInProjectRoot("var/cache/dev/appDevDebugProjectContainer.xml", containerXml);
        createFileInProjectRoot("vendor/symfony/symfony/src/Symfony/Bundle/FrameworkBundle/Resources/views/.keep", null);
        createFileInProjectRoot("vendor/foo/bar/FooBundle/Resources/views/.keep", null);
        createFileInProjectRoot("vendor/symfony/symfony/src/Symfony/Bridge/Twig/Resources/views/Form/.keep", null);
        createFileInProjectRoot("app/Resources/views/.keep", null);

        // Force cache invalidation: the debounced SymfonyVarDirectoryWatcher (300ms)
        // may not have fired yet, so cached container file lookups would return stale empty results.
        SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(getProject()).reloadConfiguration();

        Collection<TwigPath> namespaces = new ContainerTwigNamespaceExtension()
            .getNamespaces(new TwigNamespaceExtensionParameter(getProject()));

        assertFalse(namespaces.isEmpty());

        assertTrue(namespaces.stream().anyMatch(p ->
            "Framework".equals(p.getNamespace())
        ));

        assertTrue(namespaces.stream().anyMatch(p ->
            TwigUtil.MAIN.equals(p.getNamespace())
        ));

        // absolute path without kernel.project_dir is skipped — no Twig2 namespace
        assertFalse(namespaces.stream().anyMatch(p ->
            "Twig2".equals(p.getNamespace())
        ));
    }

    /**
     * Creates a file directly under project.getBaseDir() so that plugin code using
     * project.getBaseDir() for path resolution can find it (unlike myFixture.addFileToProject
     * which targets the module source root).
     */
    private void createFileInProjectRoot(String relativePath, String content) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                String[] parts = relativePath.split("/");
                VirtualFile baseDir = getProject().getBaseDir();
                VirtualFile dir = VfsUtil.createDirectoryIfMissing(
                    baseDir,
                    StringUtils.join(Arrays.copyOf(parts, parts.length - 1), "/")
                );
                VirtualFile file = dir.createChildData(this, parts[parts.length - 1]);
                if (content != null) {
                    file.setBinaryContent(content.getBytes());
                }
            } catch (IOException ignored) {
            }
        });
    }
}
