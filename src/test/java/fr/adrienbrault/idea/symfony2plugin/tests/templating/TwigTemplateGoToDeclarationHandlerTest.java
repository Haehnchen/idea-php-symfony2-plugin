package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.TwigBlockTag;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateGoToDeclarationHandler
 */
public class TwigTemplateGoToDeclarationHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("TwigTemplateGoToLocalDeclarationHandler.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     */
    public void testBlockNavigation() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        myFixture.addFileToProject("app/Resources/views/block.html.twig", "{% block foo %}{% endblock %}");

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% extends '::block.html.twig' %}{% block f<caret>oo %}",
            PlatformPatterns.psiElement(TwigBlockTag.class)
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% extends '::block.html.twig' %}{% block 'f<caret>oo' %}",
            PlatformPatterns.psiElement(TwigBlockTag.class)
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% extends '::block.html.twig' %}{% block \"f<caret>oo\" %}",
            PlatformPatterns.psiElement(TwigBlockTag.class)
        );

        assertNavigationIsEmpty(
            TwigFileType.INSTANCE,
            "{% extends '::block.html.twig' %}{% embed '::embed.html.twig' %}{% block f<caret>oo %}{% endembed %}"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     */
    public void testBlockNavigationInEmbed() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        myFixture.addFileToProject("app/Resources/views/embed.html.twig", "{% block foo_embed %}{% endblock %}");

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% extends '::block.html.twig' %}\n" +
            "{% embed '::embed.html.twig' %}\n" +
            "  {% block foo<caret>_embed %}{% endblock %}\n" +
            "{% endembed %}",
            PlatformPatterns.psiElement(TwigBlockTag.class)
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% extends '::block.html.twig' %}\n" +
            "{% embed '::embed.html.twig' %}\n" +
            "  {% if foo %}" +
            "    {% block test %}" +
            "       {% block foo<caret>_embed %}{% endblock test %}" +
            "    {% endblock %}" +
            "  {% endif %}\n" +
            "{% endembed %}",
            PlatformPatterns.psiElement(TwigBlockTag.class)
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateGoToDeclarationHandler
     */
    public void testSimpleTestNavigationToExtension() {
        myFixture.copyFileToProject("TwigFilterExtension.php");

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% if foo is bar<caret>_even %}",
            PlatformPatterns.psiElement(Function.class).withName("twig_test_even")
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% if foo is bar ev<caret>en %}",
            PlatformPatterns.psiElement(Function.class).withName("twig_test_even")
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% if foo is b<caret>ar even %}",
            PlatformPatterns.psiElement(Function.class).withName("twig_test_even")
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% if foo is not bar<caret>_even %}",
            PlatformPatterns.psiElement(Function.class).withName("twig_test_even")
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% if foo is not bar ev<caret>en %}",
            PlatformPatterns.psiElement(Function.class).withName("twig_test_even")
        );
    }

    public void testGetVarClassGoto() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{# @var bar \\Date<caret>Time #}", PlatformPatterns.psiElement(PhpClass.class));
        assertNavigationMatch(TwigFileType.INSTANCE, "{# @var \\Date<caret>Time bar #}", PlatformPatterns.psiElement(PhpClass.class));
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
            assertTrue(Pattern.compile(TwigPattern.DOC_SEE_REGEX).matcher("{# @see " + s + " #}").find());
            assertTrue(Pattern.compile(TwigPattern.DOC_SEE_REGEX).matcher("{# @see " + s + "#}").find());

            assertTrue(Pattern.compile(TwigUtil.DOC_SEE_REGEX_WITHOUT_SEE).matcher("{# " + s + " #}").find());
            assertTrue(Pattern.compile(TwigUtil.DOC_SEE_REGEX_WITHOUT_SEE).matcher("{# " + s + "#}").find());
        }
    }

    public void testThatConstantProvidesNavigation() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ constant('\\Foo\\ConstantBar\\Foo::F<caret>OO') }}", PlatformPatterns.psiElement(Field.class).withName("FOO"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ constant('\\\\Foo\\\\ConstantBar\\\\Foo::F<caret>OO') }}", PlatformPatterns.psiElement(Field.class).withName("FOO"));

        assertNavigationMatch(TwigFileType.INSTANCE, "{% if foo == constant('\\Foo\\ConstantBar\\Foo::F<caret>OO') %}", PlatformPatterns.psiElement(Field.class).withName("FOO"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{% set foo == constant('\\Foo\\ConstantBar\\Foo::F<caret>OO') %}", PlatformPatterns.psiElement(Field.class).withName("FOO"));

        assertNavigationMatch(TwigFileType.INSTANCE, "{{ constant('CONST<caret>_FOO') }}", PlatformPatterns.psiElement());
    }

    public void testTestControllerActionsProvidesReferences() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ controller('\\FooBundle\\Cont<caret>roller\\FooController::barAction') }}", PlatformPatterns.psiElement(Method.class).withName("barAction"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ controller('\\\\FooBundle\\\\Cont<caret>roller\\\\FooController::barAction') }}", PlatformPatterns.psiElement(Method.class).withName("barAction"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ controller('FooBundle\\Cont<caret>roller\\FooController::barAction') }}", PlatformPatterns.psiElement(Method.class).withName("barAction"));
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ controller('FooBundle\\\\Cont<caret>roller\\\\FooController::barAction') }}", PlatformPatterns.psiElement(Method.class).withName("barAction"));
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

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% if foo<caret>_test() %}{% endif %}", PlatformPatterns.psiElement(Function.class).withName("foo_test")
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% if %}{% else foo<caret>_test() %}{% endif %}", PlatformPatterns.psiElement(Function.class).withName("foo_test")
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% if %}{% elseif foo<caret>_test() %}{% endif %}", PlatformPatterns.psiElement(Function.class).withName("foo_test")
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% for user in foo<caret>_test() %}{% endfor %}", PlatformPatterns.psiElement(Function.class).withName("foo_test")
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% for user in users if foo<caret>_test() %}{% endfor %}", PlatformPatterns.psiElement(Function.class).withName("foo_test")
        );
    }

    public void testTokenTagNavigation() {
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% tag_<caret>foobar 'foo' %}", PlatformPatterns.psiElement()
        );
    }
}
