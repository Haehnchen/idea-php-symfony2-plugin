package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.RenderParameterGotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.RenderParameterGotoCompletionRegistrar
 */
public class RenderParameterGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testTemplateNameExtractionForFunction() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php foo('foo.html.twig', ['<caret>']);");
        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertContainsElements(RenderParameterGotoCompletionRegistrar.getTemplatesForScope(psiElement), "foo.html.twig");

        myFixture.configureByText(PhpFileType.INSTANCE, "<?php foo('foo.html.twig', ['<caret>' => 'foo']);");
        psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertContainsElements(RenderParameterGotoCompletionRegistrar.getTemplatesForScope(psiElement), "foo.html.twig");
    }
}
