package fr.adrienbrault.idea.symfony2plugin.tests.templating.path;

import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.path.GlobalAppTwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.path.GlobalAppTwigNamespaceExtension
 */
    public class GlobalAppTwigNamespaceExtensionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testThatBundleNamespacesAreAdded() {
        myFixture.addFileToProject("app/Resources/views/foo.html.twig", "");
        myFixture.addFileToProject("templates/foo.html.twig", "");

        Collection<TwigPath> namespaces = new GlobalAppTwigNamespaceExtension()
            .getNamespaces(new TwigNamespaceExtensionParameter(getProject()));

        assertNotNull(namespaces.stream()
            .filter(twigPath -> TwigUtil.MAIN.equals(twigPath.getNamespace()) && "app/Resources/views".endsWith(twigPath.getPath()))
            .findFirst()
        );

        assertNotNull(namespaces.stream()
            .filter(twigPath -> TwigUtil.MAIN.equals(twigPath.getNamespace()) && "templates".endsWith(twigPath.getPath()))
            .findFirst()
        );
    }

    public void testThatCustomAppDirectoryIsSupported() {
        Settings.getInstance(getProject()).directoryToApp = "foo/app";
        myFixture.addFileToProject("foo/app/Resources/views/foo.html.twig", "");

        Collection<TwigPath> namespaces = new GlobalAppTwigNamespaceExtension()
            .getNamespaces(new TwigNamespaceExtensionParameter(getProject()));

        assertNotNull(namespaces.stream()
            .filter(twigPath -> TwigUtil.MAIN.equals(twigPath.getNamespace()) && "app/Resources/views".endsWith(twigPath.getPath()))
            .findFirst()
        );
    }

    public void testThatCustomAppDirectoryIsSupportedForWindows() {
        Settings.getInstance(getProject()).directoryToApp = "foo\\app";
        myFixture.addFileToProject("foo/app/Resources/views/foo.html.twig", "");

        Collection<TwigPath> namespaces = new GlobalAppTwigNamespaceExtension()
            .getNamespaces(new TwigNamespaceExtensionParameter(getProject()));

        assertNotNull(namespaces.stream()
            .filter(twigPath -> TwigUtil.MAIN.equals(twigPath.getNamespace()) && "app/Resources/views".endsWith(twigPath.getPath()))
            .findFirst()
        );
    }

    public void testThatAppDirectoryInRootIsAlwaysSupported() {
        Settings.getInstance(getProject()).directoryToApp = "foo\\app";
        myFixture.addFileToProject("app/Resources/views/foo.html.twig", "");

        Collection<TwigPath> namespaces = new GlobalAppTwigNamespaceExtension()
            .getNamespaces(new TwigNamespaceExtensionParameter(getProject()));

        assertNotNull(namespaces.stream()
            .filter(twigPath -> TwigUtil.MAIN.equals(twigPath.getNamespace()) && "app/Resources/views".endsWith(twigPath.getPath()))
            .findFirst()
        );
    }
}
