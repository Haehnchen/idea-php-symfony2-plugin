package fr.adrienbrault.idea.symfony2plugin.tests;

import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathIndex;

import java.util.Arrays;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.TwigHelper
 */
public class TwigHelperTempTest extends SymfonyTempCodeInsightFixtureTestCase {
    /**
     * @see fr.adrienbrault.idea.symfony2plugin.TwigHelper#getTemplatePsiElements
     */
    public void testGetTemplatePsiElements() {
        createFile("res", "foo.html.twig");

        Settings.getInstance(getProject()).twigNamespaces.addAll(Arrays.asList(
            new TwigNamespaceSetting("Foo", "res", true, TwigPathIndex.NamespaceType.ADD_PATH, true),
            new TwigNamespaceSetting(TwigPathIndex.MAIN, "res", true, TwigPathIndex.NamespaceType.ADD_PATH, true),
            new TwigNamespaceSetting(TwigPathIndex.MAIN, "res", true, TwigPathIndex.NamespaceType.BUNDLE, true)
        ));

        String[] strings = {
            "@Foo/foo.html.twig", "@!Foo/foo.html.twig", "foo.html.twig", ":foo.html.twig", "@Foo\\foo.html.twig", "::foo.html.twig"
        };

        for (String file : strings) {
            PsiFile[] templatePsiElements = TwigHelper.getTemplatePsiElements(getProject(), file);

            assertTrue(templatePsiElements.length > 0);
            assertEquals("foo.html.twig", templatePsiElements[0].getName());
        }
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.TwigHelper#getTemplateNamesForFile
     */
    public void testGetTemplateNamesForFile() {
        Settings.getInstance(getProject()).twigNamespaces.addAll(Arrays.asList(
            new TwigNamespaceSetting("Foo", "res", true, TwigPathIndex.NamespaceType.ADD_PATH, true),
            new TwigNamespaceSetting(TwigPathIndex.MAIN, "res", true, TwigPathIndex.NamespaceType.ADD_PATH, true),
            new TwigNamespaceSetting(TwigPathIndex.MAIN, "res", true, TwigPathIndex.NamespaceType.BUNDLE, true),
            new TwigNamespaceSetting("FooBundle", "res", true, TwigPathIndex.NamespaceType.BUNDLE, true)
        ));

        assertContainsElements(
            TwigHelper.getTemplateNamesForFile(getProject(), createFile("res", "test.html.twig")),
            "@Foo/test.html.twig", "test.html.twig", "::test.html.twig", "FooBundle::test.html.twig"
        );

        assertContainsElements(
            TwigHelper.getTemplateNamesForFile(getProject(), createFile("res/foobar", "test.html.twig")),
            "@Foo/foobar/test.html.twig", "foobar/test.html.twig", ":foobar:test.html.twig", "FooBundle:foobar:test.html.twig"
        );
    }
}
