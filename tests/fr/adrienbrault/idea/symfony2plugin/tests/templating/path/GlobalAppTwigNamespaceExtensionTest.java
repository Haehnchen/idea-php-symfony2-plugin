package fr.adrienbrault.idea.symfony2plugin.tests.templating.path;

import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.path.GlobalAppTwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.path.GlobalAppTwigNamespaceExtension
 */
public class GlobalAppTwigNamespaceExtensionTest extends SymfonyLightCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        createDummyFiles("app/Resources/views/foo.html.twig");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testThatBundleNamespacesAreAdded() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        Collection<TwigPath> namespaces = new GlobalAppTwigNamespaceExtension()
            .getNamespaces(new TwigNamespaceExtensionParameter(getProject()));

        assertNotNull(ContainerUtil.find(namespaces, twigPath ->
            TwigPathIndex.MAIN.equals(twigPath.getNamespace()) && "app/Resources/views".equals(twigPath.getPath()))
        );

        assertNotNull(ContainerUtil.find(namespaces, twigPath ->
            TwigPathIndex.MAIN.equals(twigPath.getNamespace()) && "app/Resources/views".equals(twigPath.getPath()))
        );
    }
}
