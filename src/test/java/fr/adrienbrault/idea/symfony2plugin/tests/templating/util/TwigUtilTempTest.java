package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlPsiElementFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigUtilTempTest extends SymfonyTempCodeInsightFixtureTestCase {
    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getPresentableTemplateName
     */
    public void testGetPresentableTemplateName() {
        VirtualFile res = createFile("res/foo/foo.html.twig");


        Settings.getInstance(getProject()).twigNamespaces.addAll(Collections.singletonList(
            new TwigNamespaceSetting("Foobar", "res", true, TwigUtil.NamespaceType.BUNDLE, true)
        ));

        PsiFile file = PsiManager.getInstance(getProject()).findFile(res);
        assertEquals("Foobar:foo:foo", TwigUtil.getPresentableTemplateName(file, true));
        assertEquals("Foobar:foo:foo.html.twig", TwigUtil.getPresentableTemplateName(file, false));
    }

    /**
     * @see TwigUtil#getTemplatePsiElements
     */
    public void testGetTemplatePsiElements() {
        createFile("res/foo.html.twig");

        Settings.getInstance(getProject()).twigNamespaces.addAll(Arrays.asList(
            new TwigNamespaceSetting("Foo", "res", true, TwigUtil.NamespaceType.ADD_PATH, true),
            new TwigNamespaceSetting(TwigUtil.MAIN, "res", true, TwigUtil.NamespaceType.ADD_PATH, true),
            new TwigNamespaceSetting(TwigUtil.MAIN, "res", true, TwigUtil.NamespaceType.BUNDLE, true)
        ));

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
            TwigUtil.getTemplateNamesForFile(getProject(), createFile("res/test.html.twig")),
            "@Foo/test.html.twig", "test.html.twig", "::test.html.twig", "FooBundle::test.html.twig"
        );

        assertContainsElements(
            TwigUtil.getTemplateNamesForFile(getProject(), createFile("res/foobar/test.html.twig")),
            "@Foo/foobar/test.html.twig", "foobar/test.html.twig", ":foobar:test.html.twig", "FooBundle:foobar:test.html.twig"
        );
    }

    public void testGetTwigFileNames() {
        createFile("res/foobar/foo.html.twig");

        Settings.getInstance(getProject()).twigNamespaces.addAll(createTwigNamespaceSettings());

        assertContainsElements(
            TwigUtil.getTemplateMap(getProject()).keySet(),
            "@Foo/foobar/foo.html.twig", "FooBundle:foobar:foo.html.twig", ":foobar:foo.html.twig", "foobar/foo.html.twig"
        );
    }

    public void testGetTwigAndPhpTemplateFiles() {
        createFiles("res/foobar/foo.html.twig", "res/foobar/foo.php");

        Settings.getInstance(getProject()).twigNamespaces.addAll(createTwigNamespaceSettings());

        assertContainsElements(
            TwigUtil.getTemplateMap(getProject(), true).keySet(),
            "@Foo/foobar/foo.html.twig", "FooBundle:foobar:foo.html.twig", ":foobar:foo.html.twig", "foobar/foo.html.twig",
            "@Foo/foobar/foo.php", "FooBundle:foobar:foo.php", ":foobar:foo.php", "foobar/foo.php"
        );
    }

    /**
     * @see TwigUtil#getTemplateNavigationOnOffset
     */
    public void testGetTemplateNavigationOnOffset() {
        createFiles("res/foobar/foo.html.twig");

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
        createFiles("res/foobar/foo.html.twig");
        createFiles("res/foobar/apple/foo.html.twig");

        Settings.getInstance(getProject()).twigNamespaces.addAll(createTwigNamespaceSettings());

        assertIsDirectoryAtOffset("@Foo/foobar/foo.html.twig", 2, "res");
        assertIsDirectoryAtOffset("@Foo/foobar\\foo.html.twig", 6, "foobar");

        assertIsDirectoryAtOffset( "foobar/foo.html.twig", 3, "foobar");
        assertIsDirectoryAtOffset( "foobar/apple/foo.html.twig", 9, "apple");
        assertIsDirectoryAtOffset( "foobar\\apple\\foo.html.twig", 9, "apple");

        assertIsDirectoryAtOffset("@Foo/foobar/foo.html.twig", 6, "foobar");
        assertIsDirectoryAtOffset("@Foo/foobar\\foo.html.twig", 6, "foobar");
        assertIsDirectoryAtOffset("@Foo/foobar/apple/foo.html.twig", 15, "apple");

        assertIsDirectoryAtOffset( "@Foo/foobar/foo.html.twig", 3, "res");
        assertIsDirectoryAtOffset("FooBundle:foobar:foo.html.twig", 6, "res");
        assertIsDirectoryAtOffset("FooBundle:foobar:foo.html.twig", 13, "foobar");
        assertIsDirectoryAtOffset(":foobar:foo.php", 4, "foobar");

        assertIsDirectoryAtOffset(":foobar/apple:foo.php", 10, "apple");
        assertIsDirectoryAtOffset(":foobar\\apple:foo.php", 10, "apple");
        assertIsDirectoryAtOffset(":foobar\\apple\\foo.php", 10, "apple");

        assertTrue(TwigUtil.getTemplateTargetOnOffset(getProject(), "@Foo/foobar/foo.html.twig", 15).isEmpty());
        assertTrue(TwigUtil.getTemplateTargetOnOffset(getProject(), "foo.html.twig", 40).isEmpty());
    }

    /**
     * @see TwigUtil#getTwigPathFromYamlConfigResolved
     */
    public void testGetTwigPathFromYamlConfigResolved() {
        createFile("app/test/foo.html.twig");
        createFile("app/template/foo.html.twig");

        PsiFile dummyFile = YamlPsiElementFactory.createDummyFile(getProject(), "" +
            "twig:\n" +
            "   paths:\n" +
            "       '%kernel.root_dir%/test': foo\n" +
            "       '%kernel.project_dir%/app/test': project\n" +
            "       '%kernel.root_dir%/../app': app\n" +
            "       'app/template': MY_PREFIX\n" +
            "       'app///\\\\\\template': MY_PREFIX_1\n"
        );

        Collection<Pair<String, String>> paths = TwigUtil.getTwigPathFromYamlConfigResolved((YAMLFile) dummyFile);

        assertNotNull(
            paths.stream().filter(pair -> "foo".equals(pair.getFirst()) && "app/test".equals(pair.getSecond())).findFirst().get()
        );

        assertNotNull(
            paths.stream().filter(pair -> "project".equals(pair.getFirst()) && "app/test".equals(pair.getSecond())).findFirst().get()
        );

        assertNotNull(
            paths.stream().filter(pair -> "app".equals(pair.getFirst()) && "app".equals(pair.getSecond())).findFirst().get()
        );

        assertNotNull(
            paths.stream().filter(pair -> "MY_PREFIX".equals(pair.getFirst()) && "app/template".equals(pair.getSecond())).findFirst().get()
        );

        Pair<String, String> myPrefix1 = paths.stream().filter(pair -> "MY_PREFIX_1".equals(pair.getFirst())).findAny().get();

        assertEquals("MY_PREFIX_1", myPrefix1.getFirst());
        assertEquals("app/template", myPrefix1.getSecond());
    }

    private void assertIsDirectoryAtOffset(@NotNull String templateName, int offset, @NotNull String directory) {
        assertTrue(TwigUtil.getTemplateTargetOnOffset(getProject(), templateName, offset).stream().anyMatch(psiElement -> psiElement instanceof PsiDirectory && directory.equals(((PsiDirectory) psiElement).getName())));
    }

    @NotNull
    private List<TwigNamespaceSetting> createTwigNamespaceSettings() {
        return Arrays.asList(
            new TwigNamespaceSetting("Foo", "res", true, TwigUtil.NamespaceType.ADD_PATH, true),
            new TwigNamespaceSetting(TwigUtil.MAIN, "res", true, TwigUtil.NamespaceType.ADD_PATH, true),
            new TwigNamespaceSetting(TwigUtil.MAIN, "res", true, TwigUtil.NamespaceType.BUNDLE, true),
            new TwigNamespaceSetting("FooBundle", "res", true, TwigUtil.NamespaceType.BUNDLE, true)
        );
    }
}
