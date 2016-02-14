package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public void testSeeTagGoto() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{# @see \\Date<caret>Time #}", PlatformPatterns.psiElement(PhpClass.class));
        assertNavigationMatch(TwigFileType.INSTANCE, "{# @see Date<caret>Time #}", PlatformPatterns.psiElement(PhpClass.class));
        assertNavigationMatch(TwigFileType.INSTANCE, "{# @see Date<caret>Time::format #}", PlatformPatterns.psiElement(Method.class));
        assertNavigationMatch(TwigFileType.INSTANCE, "{# @see \\Date<caret>Time:format #}", PlatformPatterns.psiElement(Method.class));
        assertNavigationMatch(TwigFileType.INSTANCE, "{# \\Date<caret>Time:format #}", PlatformPatterns.psiElement(Method.class));
    }

    public void testSeeTagGotoRegexMatch() {
        for (String s : new String[]{"\\DateTime", "\\DateTime::format", "foo/foo.html.twig", "@foo/foo.html.twig", "@foo/fo-o.html.twig", "@foo\\fo-o.html.twig", "Include/type_embed.html.twig"}) {
            assertTrue(Pattern.compile(TwigHelper.DOC_SEE_REGEX).matcher("{# @see " + s + " #}").find());
            assertTrue(Pattern.compile(TwigHelper.DOC_SEE_REGEX).matcher("{# @see " + s + "#}").find());

            assertTrue(Pattern.compile(TwigHelper.DOC_SEE_REGEX_WITHOUT_SEE).matcher("{# " + s + " #}").find());
            assertTrue(Pattern.compile(TwigHelper.DOC_SEE_REGEX_WITHOUT_SEE).matcher("{# " + s + "#}").find());
        }
    }
}
