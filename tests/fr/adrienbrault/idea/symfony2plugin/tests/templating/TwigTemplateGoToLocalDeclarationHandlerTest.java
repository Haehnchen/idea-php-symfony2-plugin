package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateGoToLocalDeclarationHandler
 */
public class TwigTemplateGoToLocalDeclarationHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testGetVarClassGoto() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{# @var bar \\Date<caret>Time #}", PlatformPatterns.psiElement(PhpClass.class));
    }

    public void testGetVarClassGotoDeprecated() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{# bar \\Date<caret>Time #}", PlatformPatterns.psiElement(PhpClass.class));
    }
}
