package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateGoToLocalDeclarationHandler
 */
public class TwigTemplateGoToLocalDeclarationHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("TwigTemplateGoToLocalDeclarationHandler.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
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

    public void testThatConstantProvidesNavigation() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ constant('\\Foo\\ConstantBar\\Foo::F<caret>OO') }}", PlatformPatterns.psiElement(Field.class).withName("FOO"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ constant('\\\\Foo\\\\ConstantBar\\\\Foo::F<caret>OO') }}", PlatformPatterns.psiElement(Field.class).withName("FOO"));

        assertNavigationMatch(TwigFileType.INSTANCE, "{% if foo == constant('\\Foo\\ConstantBar\\Foo::F<caret>OO') %}", PlatformPatterns.psiElement(Field.class).withName("FOO"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{% set foo == constant('\\Foo\\ConstantBar\\Foo::F<caret>OO') %}", PlatformPatterns.psiElement(Field.class).withName("FOO"));

        assertNavigationMatch(TwigFileType.INSTANCE, "{{ constant('CONST<caret>_FOO') }}", PlatformPatterns.psiElement());
    }

    public void testFunctionNavigation() {
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ foo<caret>_test() }}", PlatformPatterns.psiElement(Function.class).withName("foo_test")
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ foo<caret>_test }}", PlatformPatterns.psiElement(Function.class).withName("foo_test")
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% set foo = foo<caret>_test %}", PlatformPatterns.psiElement(Function.class).withName("foo_test")
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% set foo = foo<caret>_test() %}", PlatformPatterns.psiElement(Function.class).withName("foo_test")
        );
    }

    public void testTokenTagNavigation() {
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% tag_<caret>foobar 'foo' %}", PlatformPatterns.psiElement()
        );
    }
}
