package fr.adrienbrault.idea.symfony2plugin.tests.templating.dict;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlockParser;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

import java.util.Arrays;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigBlockParserTest extends SymfonyTempCodeInsightFixtureTestCase {
    /**
     * fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlockParser#visit
     */
    public void testVisit() {
        VirtualFile file = createFile("res", "foo.html.twig", "{% extends \"foo1.html.twig\" %}{% block foo %}{% endblock %}");
        createFile("res", "foo1.html.twig", "{% block foo1 %}{% endblock %}");

        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);

        Settings.getInstance(getProject()).twigNamespaces.addAll(Arrays.asList(
            new TwigNamespaceSetting(TwigPathIndex.MAIN, "res", true, TwigPathIndex.NamespaceType.ADD_PATH, true)
        ));

        List<TwigBlock> walk = new TwigBlockParser(true).visit(new PsiFile[] {psiFile});

        assertNotNull(walk.stream().filter(twigBlock -> "foo".equals(twigBlock.getName())).findFirst().get());
        assertNotNull(walk.stream().filter(twigBlock -> "foo1".equals(twigBlock.getName())).findFirst().get());
    }

    /**
     * fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlockParser#walk
     */
    public void testVisitNotForSelf() {
        VirtualFile file = createFile("res", "foo.html.twig", "{% extends \"foo1.html.twig\" %}{% block foo %}{% endblock %}");
        createFile("res", "foo1.html.twig", "{% block foo1 %}{% endblock %}");

        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);

        Settings.getInstance(getProject()).twigNamespaces.addAll(Arrays.asList(
            new TwigNamespaceSetting(TwigPathIndex.MAIN, "res", true, TwigPathIndex.NamespaceType.ADD_PATH, true)
        ));

        List<TwigBlock> walk = new TwigBlockParser().visit(new PsiFile[] {psiFile});

        assertEquals(0, walk.stream().filter(twigBlock -> "foo".equals(twigBlock.getName())).count());
        assertNotNull(walk.stream().filter(twigBlock -> "foo1".equals(twigBlock.getName())).findFirst().get());
    }

    /**
     * fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlockParser#walk
     */
    public void testWalkWithSelf() {
        VirtualFile file = createFile("res", "foo.html.twig", "{% extends \"foo1.html.twig\" %}{% block foo %}{% endblock %}");
        createFile("res", "foo1.html.twig", "{% block foo1 %}{% endblock %}");

        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);

        Settings.getInstance(getProject()).twigNamespaces.addAll(Arrays.asList(
            new TwigNamespaceSetting(TwigPathIndex.MAIN, "res", true, TwigPathIndex.NamespaceType.ADD_PATH, true)
        ));

        List<TwigBlock> walk = new TwigBlockParser(true).walk(psiFile);

        assertEquals(1, walk.stream().filter(twigBlock -> "foo".equals(twigBlock.getName()) && "self".equals(twigBlock.getShortcutName())).count());
        assertNotNull(walk.stream().filter(twigBlock -> "foo1".equals(twigBlock.getName())).findFirst().get());
    }
}
