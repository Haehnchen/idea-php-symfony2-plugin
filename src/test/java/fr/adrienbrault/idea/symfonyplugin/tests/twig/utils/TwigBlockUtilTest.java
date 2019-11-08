package fr.adrienbrault.idea.symfonyplugin.tests.twig.utils;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import fr.adrienbrault.idea.symfonyplugin.Settings;
import fr.adrienbrault.idea.symfonyplugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfonyplugin.templating.path.TwigNamespaceSetting;
import fr.adrienbrault.idea.symfonyplugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyTempCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfonyplugin.twig.utils.TwigBlockUtil;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.twig.utils.TwigBlockUtil
 */
public class TwigBlockUtilTest extends SymfonyTempCodeInsightFixtureTestCase {
    /**
     * fr.adrienbrault.idea.symfonyplugin.twig.utils.TwigBlockUtil#collectParentBlocks
     */
    public void testVisit() {
        // skip for no fully project
        if(true) { return; }

        VirtualFile file = createFile("res/foo.html.twig", "{% extends \"foo1.html.twig\" %}{% block foo %}{% endblock %}");
        createFile("res/foo1.html.twig", "{% block foo1 %}{% endblock %}");

        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);

        Settings.getInstance(getProject()).twigNamespaces.addAll(Collections.singletonList(
            new TwigNamespaceSetting(TwigUtil.MAIN, "res", true, TwigUtil.NamespaceType.ADD_PATH, true)
        ));

        Collection<TwigBlock> walk = TwigBlockUtil.collectParentBlocks(true, psiFile);

        assertNotNull(walk.stream().filter(twigBlock -> "foo".equals(twigBlock.getName())).findFirst().get());
        assertNotNull(walk.stream().filter(twigBlock -> "foo1".equals(twigBlock.getName())).findFirst().get());
    }

    /**
     * fr.adrienbrault.idea.symfonyplugin.twig.utils.TwigBlockUtil#collectParentBlocks
     */
    public void testVisitNotForSelf() {
        // skip for no fully project
        if(true) { return; }

        VirtualFile file = createFile("res/foo.html.twig", "{% extends \"foo1.html.twig\" %}{% block foo %}{% endblock %}");
        createFile("res/foo1.html.twig", "{% block foo1 %}{% endblock %}");

        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);

        Settings.getInstance(getProject()).twigNamespaces.addAll(Collections.singletonList(
            new TwigNamespaceSetting(TwigUtil.MAIN, "res", true, TwigUtil.NamespaceType.ADD_PATH, true)
        ));

        Collection<TwigBlock> walk = TwigBlockUtil.collectParentBlocks(false, psiFile);

        assertEquals(0, walk.stream().filter(twigBlock -> "foo".equals(twigBlock.getName())).count());
        assertNotNull(walk.stream().filter(twigBlock -> "foo1".equals(twigBlock.getName())).findFirst().get());
    }

    /**
     * fr.adrienbrault.idea.symfonyplugin.twig.utils.TwigBlockUtil#collectParentBlocks
     */
    public void testWalkWithSelf() {
        // skip for no fully project
        if(true) { return; }

        VirtualFile file = createFile("res/foo.html.twig", "{% extends \"foo1.html.twig\" %}{% block foo %}{% endblock %}");
        createFile("res/foo1.html.twig", "{% block foo1 %}{% endblock %}");

        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);

        Settings.getInstance(getProject()).twigNamespaces.addAll(Collections.singletonList(
            new TwigNamespaceSetting(TwigUtil.MAIN, "res", true, TwigUtil.NamespaceType.ADD_PATH, true)
        ));

        Collection<TwigBlock> walk = TwigBlockUtil.collectParentBlocks(true, psiFile);

        assertEquals(1, walk.stream().filter(twigBlock -> "foo".equals(twigBlock.getName())).count());
        assertNotNull(walk.stream().filter(twigBlock -> "foo1".equals(twigBlock.getName())).findFirst().get());
    }
}
