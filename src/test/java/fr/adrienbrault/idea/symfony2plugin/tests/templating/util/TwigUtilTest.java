package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigUtil
 */
public class TwigUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    /**
     * @see TwigUtil#getTemplatePsiElements
     */
    public void testGetTemplatePsiElements() {
        myFixture.addFileToProject("res/foo.html.twig", "");

        Settings.getInstance(getProject()).twigNamespaces.addAll(createTwigNamespaceSettings());

        String[] strings = {
            "@Foo/foo.html.twig", "@!Foo/foo.html.twig", "foo.html.twig", ":foo.html.twig", "@Foo\\foo.html.twig", "::foo.html.twig"
        };

        for (String file : strings) {
            Collection<PsiFile> templatePsiElements = TwigUtil.getTemplatePsiElements(getProject(), file);

            assertTrue(!templatePsiElements.isEmpty());
            assertEquals("foo.html.twig", templatePsiElements.iterator().next().getName());
        }
    }

    /**
     * @see TwigUtil#getTemplateNamesForFile
     */
    public void testGetTemplateNamesForFile() {
        Settings.getInstance(getProject()).twigNamespaces.addAll(createTwigNamespaceSettings());

        assertContainsElements(
            TwigUtil.getTemplateNamesForFile(getProject(), myFixture.addFileToProject("res/test.html.twig", "").getVirtualFile()),
            "@Foo/test.html.twig", "test.html.twig", "::test.html.twig", "FooBundle::test.html.twig"
        );

        assertContainsElements(
            TwigUtil.getTemplateNamesForFile(getProject(), myFixture.addFileToProject("res/foobar/test.html.twig", "").getVirtualFile()),
            "@Foo/foobar/test.html.twig", "foobar/test.html.twig", ":foobar:test.html.twig", "FooBundle:foobar:test.html.twig"
        );
    }

    /**
     * @see TwigUtil#getTemplateNavigationOnOffset
     */
    public void testGetTemplateNavigationOnOffset() {
        myFixture.addFileToProject("res/foobar/foo.html.twig", "");

        Settings.getInstance(getProject()).twigNamespaces.addAll(createTwigNamespaceSettings());

        assertTrue(TwigUtil.getTemplateNavigationOnOffset(getProject(), "foobar/foo.html.twig", 3).stream().anyMatch(psiElement -> psiElement instanceof PsiDirectory && "foobar".equals(((PsiDirectory) psiElement).getName())));
        assertTrue(TwigUtil.getTemplateNavigationOnOffset(getProject(), ":foobar:foo.html.twig", 3).stream().anyMatch(psiElement -> psiElement instanceof PsiDirectory && "foobar".equals(((PsiDirectory) psiElement).getName())));

        assertTrue(TwigUtil.getTemplateNavigationOnOffset(getProject(), "foobar/foo.html.twig", 10).stream().anyMatch(psiElement -> psiElement instanceof PsiFile && "foo.html.twig".equals(((PsiFile) psiElement).getName())));

        assertTrue(TwigUtil.getTemplateTargetOnOffset(getProject(), "foo.html.twig", 40).isEmpty());
    }

    /**
     * @see TwigUtil#getTemplateTargetOnOffset
     */
    public void testGetTemplateTargetOnOffset() {
        myFixture.addFileToProject("res/foobar/foo.html.twig", "");
        myFixture.addFileToProject("res/foobar/apple/foo.html.twig", "");

        Settings.getInstance(getProject()).twigNamespaces.addAll(createTwigNamespaceSettings());

        assertIsDirectoryAtOffset("@Foo/foobar/foo.html.twig", 2, "res");
        assertIsDirectoryAtOffset("@Foo/foobar\\foo.html.twig", 6, "foobar");

        assertIsDirectoryAtOffset("foobar/foo.html.twig", 3, "foobar");
        assertIsDirectoryAtOffset("foobar/apple/foo.html.twig", 9, "apple");
        assertIsDirectoryAtOffset("foobar\\apple\\foo.html.twig", 9, "apple");

        assertIsDirectoryAtOffset("@Foo/foobar/foo.html.twig", 6, "foobar");
        assertIsDirectoryAtOffset("@Foo/foobar\\foo.html.twig", 6, "foobar");
        assertIsDirectoryAtOffset("@Foo/foobar/apple/foo.html.twig", 15, "apple");

        assertIsDirectoryAtOffset("@Foo/foobar/foo.html.twig", 3, "res");
        assertIsDirectoryAtOffset("FooBundle:foobar:foo.html.twig", 6, "res");
        assertIsDirectoryAtOffset("FooBundle:foobar:foo.html.twig", 13, "foobar");
        assertIsDirectoryAtOffset(":foobar:foo.php", 4, "foobar");

        assertIsDirectoryAtOffset(":foobar/apple:foo.php", 10, "apple");
        assertIsDirectoryAtOffset(":foobar\\apple:foo.php", 10, "apple");
        assertIsDirectoryAtOffset(":foobar\\apple\\foo.php", 10, "apple");

        assertTrue(TwigUtil.getTemplateTargetOnOffset(getProject(), "@Foo/foobar/foo.html.twig", 15).isEmpty());
        assertTrue(TwigUtil.getTemplateTargetOnOffset(getProject(), "foo.html.twig", 40).isEmpty());
    }

    private void assertIsDirectoryAtOffset(@NotNull String templateName, int offset, @NotNull String directory) {
        assertTrue(TwigUtil.getTemplateTargetOnOffset(getProject(), templateName, offset).stream().anyMatch(psiElement -> psiElement instanceof PsiDirectory && directory.equals(((PsiDirectory) psiElement).getName())));
    }

    @NotNull
    private static List<TwigNamespaceSetting> createTwigNamespaceSettings() {
        return Arrays.asList(
            new TwigNamespaceSetting("Foo", "res", true, TwigUtil.NamespaceType.ADD_PATH, true),
            new TwigNamespaceSetting(TwigUtil.MAIN, "res", true, TwigUtil.NamespaceType.ADD_PATH, true),
            new TwigNamespaceSetting(TwigUtil.MAIN, "res", true, TwigUtil.NamespaceType.BUNDLE, true),
            new TwigNamespaceSetting("FooBundle", "res", true, TwigUtil.NamespaceType.BUNDLE, true)
        );
    }
}
