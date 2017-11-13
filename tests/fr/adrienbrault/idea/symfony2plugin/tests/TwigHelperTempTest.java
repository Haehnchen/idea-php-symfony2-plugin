package fr.adrienbrault.idea.symfony2plugin.tests;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

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
        createFile("res/foo.html.twig");

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
        Settings.getInstance(getProject()).twigNamespaces.addAll(createTwigNamespaceSettings());

        assertContainsElements(
            TwigHelper.getTemplateNamesForFile(getProject(), createFile("res/test.html.twig")),
            "@Foo/test.html.twig", "test.html.twig", "::test.html.twig", "FooBundle::test.html.twig"
        );

        assertContainsElements(
            TwigHelper.getTemplateNamesForFile(getProject(), createFile("res/foobar/test.html.twig")),
            "@Foo/foobar/test.html.twig", "foobar/test.html.twig", ":foobar:test.html.twig", "FooBundle:foobar:test.html.twig"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.TwigHelper#getTwigFileNames
     */
    public void testGetTwigFileNames() {
        createFile("res/foobar/foo.html.twig");

        Settings.getInstance(getProject()).twigNamespaces.addAll(createTwigNamespaceSettings());

        assertContainsElements(
            TwigHelper.getTwigFileNames(getProject()),
            "@Foo/foobar/foo.html.twig", "FooBundle:foobar:foo.html.twig", ":foobar:foo.html.twig", "foobar/foo.html.twig"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.TwigHelper#getTwigAndPhpTemplateFiles
     */
    public void testGetTwigAndPhpTemplateFiles() {
        createFiles("res/foobar/foo.html.twig", "res/foobar/foo.php");

        Settings.getInstance(getProject()).twigNamespaces.addAll(createTwigNamespaceSettings());

        assertContainsElements(
            TwigHelper.getTwigAndPhpTemplateFiles(getProject()).keySet(),
            "@Foo/foobar/foo.html.twig", "FooBundle:foobar:foo.html.twig", ":foobar:foo.html.twig", "foobar/foo.html.twig",
            "@Foo/foobar/foo.php", "FooBundle:foobar:foo.php", ":foobar:foo.php", "foobar/foo.php"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.TwigHelper#getTemplateNavigationOnOffset
     */
    public void testGetTemplateNavigationOnOffset() {
        createFiles("res/foobar/foo.html.twig");

        Settings.getInstance(getProject()).twigNamespaces.addAll(createTwigNamespaceSettings());

        assertTrue(TwigHelper.getTemplateNavigationOnOffset(getProject(), "foobar/foo.html.twig", 3).stream().filter(psiElement -> psiElement instanceof PsiDirectory && "foobar".equals(((PsiDirectory) psiElement).getName())).count() > 0);
        assertTrue(TwigHelper.getTemplateNavigationOnOffset(getProject(), ":foobar:foo.html.twig", 3).stream().filter(psiElement -> psiElement instanceof PsiDirectory && "foobar".equals(((PsiDirectory) psiElement).getName())).count() > 0);

        assertTrue(TwigHelper.getTemplateNavigationOnOffset(getProject(), "foobar/foo.html.twig", 10).stream().filter(psiElement -> psiElement instanceof PsiFile && "foo.html.twig".equals(((PsiFile) psiElement).getName())).count() > 0);

        assertTrue(TwigHelper.getTemplateTargetOnOffset(getProject(), "foo.html.twig", 40).size() == 0);
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.TwigHelper#getTemplateTargetOnOffset
     */
    public void testGetTemplateTargetOnOffset() {
        createFiles("res/foobar/foo.html.twig");
        createFiles("res/foobar/apple/foo.html.twig");

        Settings.getInstance(getProject()).twigNamespaces.addAll(createTwigNamespaceSettings());

        assertIsDirectoryAtOffset("@Foo/foobar/foo.html.twig", 2, "res");
        assertIsDirectoryAtOffset("@Foo/foobar\\foo.html.twig", 6, "foobar");

        assertIsDirectoryAtOffset( "foobar/foo.html.twig", 3, "foobar");
        assertIsDirectoryAtOffset("@Foo/foobar/foo.html.twig", 6, "foobar");
        assertIsDirectoryAtOffset("@Foo/foobar\\foo.html.twig", 6, "foobar");

        assertIsDirectoryAtOffset( "@Foo/foobar/foo.html.twig", 3, "res");
        assertIsDirectoryAtOffset("FooBundle:foobar:foo.html.twig", 6, "res");
        assertIsDirectoryAtOffset("FooBundle:foobar:foo.html.twig", 13, "foobar");
        assertIsDirectoryAtOffset(":foobar:foo.php", 4, "foobar");

        assertIsDirectoryAtOffset(":foobar/apple:foo.php", 10, "apple");
        assertIsDirectoryAtOffset(":foobar\\apple:foo.php", 10, "apple");
        assertIsDirectoryAtOffset(":foobar\\apple\\foo.php", 10, "apple");

        assertTrue(TwigHelper.getTemplateTargetOnOffset(getProject(), "@Foo/foobar/foo.html.twig", 15).size() == 0);
        assertTrue(TwigHelper.getTemplateTargetOnOffset(getProject(), "foo.html.twig", 40).size() == 0);
    }

    private void assertIsDirectoryAtOffset(@NotNull String templateName, int offset, @NotNull String directory) {
        assertTrue(TwigHelper.getTemplateTargetOnOffset(getProject(), templateName, offset).stream().filter(psiElement -> psiElement instanceof PsiDirectory && directory.equals(((PsiDirectory) psiElement).getName())).count() > 0);
    }

    @NotNull
    private List<TwigNamespaceSetting> createTwigNamespaceSettings() {
        return Arrays.asList(
            new TwigNamespaceSetting("Foo", "res", true, TwigPathIndex.NamespaceType.ADD_PATH, true),
            new TwigNamespaceSetting(TwigPathIndex.MAIN, "res", true, TwigPathIndex.NamespaceType.ADD_PATH, true),
            new TwigNamespaceSetting(TwigPathIndex.MAIN, "res", true, TwigPathIndex.NamespaceType.BUNDLE, true),
            new TwigNamespaceSetting("FooBundle", "res", true, TwigPathIndex.NamespaceType.BUNDLE, true)
        );
    }
}
