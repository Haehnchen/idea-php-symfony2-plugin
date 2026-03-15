package fr.adrienbrault.idea.symfony2plugin.tests.templating.path;

import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.path.ContainerTwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.path.ContainerTwigNamespaceExtension
 */
public class ContainerTwigNamespaceExtensionTest extends SymfonyTempCodeInsightFixtureTestCase {

    public void testGetNamespacesFromContainer() throws IOException {
        String containerXml = new String(Files.readAllBytes(Paths.get(
            "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/path/appDevDebugProjectContainer.xml"
        )));

        createFile("var/cache/dev/appDevDebugProjectContainer.xml", containerXml);
        createFile("vendor/symfony/symfony/src/Symfony/Bundle/FrameworkBundle/Resources/views/.keep");
        createFile("vendor/foo/bar/FooBundle/Resources/views/.keep");
        createFile("vendor/symfony/symfony/src/Symfony/Bridge/Twig/Resources/views/Form/.keep");
        createFile("app/Resources/views/.keep");

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
}
