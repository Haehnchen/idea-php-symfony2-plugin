package fr.adrienbrault.idea.symfony2plugin.tests.navigation;

import com.intellij.navigation.GotoRelatedItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.navigation.TwigGotoRelatedProvider;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.navigation.TwigGotoRelatedProvider
 */
public class TwigGotoRelatedProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject("ide-twig.json",
            "{\"namespaces\":[{\"namespace\":\"\",\"path\":\"templates\"}]}"
        );
    }

    /**
     * @see TwigGotoRelatedProvider#getItems(PsiElement)
     */
    public void testExtendsReturnsParentTemplate() {
        myFixture.addFileToProject("templates/base.html.twig", "base");
        PsiFile childFile = myFixture.addFileToProject("templates/child.html.twig", "{% extends 'base.html.twig' %}");

        PsiElement element = childFile.getFirstChild();
        assertNotNull(element);

        List<? extends GotoRelatedItem> items = new TwigGotoRelatedProvider().getItems(element);

        assertTrue(
            items.stream().anyMatch(item ->
                item.getElement() instanceof PsiFile && "base.html.twig".equals(((PsiFile) item.getElement()).getName())
            )
        );
    }

    /**
     * @see TwigGotoRelatedProvider#getItems(PsiElement)
     */
    public void testNoExtendsReturnsEmptyForExtends() {
        PsiFile standaloneFile = myFixture.addFileToProject("templates/standalone.html.twig", "no extends here");

        PsiElement element = standaloneFile.getFirstChild();
        assertNotNull(element);

        List<? extends GotoRelatedItem> items = new TwigGotoRelatedProvider().getItems(element);

        assertTrue(
            items.stream().noneMatch(item ->
                item.getElement() instanceof PsiFile && "extends".equals(item.getMnemonic())
            )
        );
    }
}
