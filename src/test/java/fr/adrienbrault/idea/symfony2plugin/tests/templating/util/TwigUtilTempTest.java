package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlPsiElementFactory;
import org.jetbrains.annotations.NotNull;
import com.intellij.psi.PsiFile;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigUtilTempTest extends SymfonyTempCodeInsightFixtureTestCase {

    /**
     * @see TwigUtil#getPresentableTemplateName
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

    @NotNull
    static List<TwigNamespaceSetting> createTwigNamespaceSettings() {
        return Arrays.asList(
            new TwigNamespaceSetting("Foo", "res", true, TwigUtil.NamespaceType.ADD_PATH, true),
            new TwigNamespaceSetting(TwigUtil.MAIN, "res", true, TwigUtil.NamespaceType.ADD_PATH, true),
            new TwigNamespaceSetting(TwigUtil.MAIN, "res", true, TwigUtil.NamespaceType.BUNDLE, true),
            new TwigNamespaceSetting("FooBundle", "res", true, TwigUtil.NamespaceType.BUNDLE, true)
        );
    }
}
