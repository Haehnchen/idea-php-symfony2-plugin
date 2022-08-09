package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
 */
public class TwigTemplateCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("TwigTemplateCompletionContributorTest.php");
        myFixture.copyFileToProject("routing.xml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     */
    public void testBlockCompletion() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        myFixture.addFileToProject("app/Resources/views/block.html.twig", "{% block foo %}{% endblock %}");

        assertCompletionContains(TwigFileType.INSTANCE, "{% extends '::block.html.twig' %}{% block <caret> %}", "foo");
        assertCompletionContains(TwigFileType.INSTANCE, "{% extends '::block.html.twig' %}{% block \"<caret>\" %}", "foo");
        assertCompletionContains(TwigFileType.INSTANCE, "{% extends '::block.html.twig' %}{% block '<caret>' %}", "foo");

        assertCompletionNotContains(TwigFileType.INSTANCE, "" +
                "{% extends '::block.html.twig' %}\n" +
                "{% embed '::foobar.html.twig' %}\n" +
                "   {% block '<caret>' %}\n" +
                "{% endembed %}\n",
            "foo"
        );
    }

    public void testBlockCompletionForEmbed() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        myFixture.addFileToProject("app/Resources/views/embed.html.twig", "{% block foo_embed %}{% endblock %}");

        assertCompletionContains(TwigFileType.INSTANCE, "" +
                "{% embed '::embed.html.twig' %}\n" +
                "   {% block '<caret>' %}\n" +
                "{% endembed %}\n",
            "foo_embed"
        );

        assertCompletionNotContains(TwigFileType.INSTANCE, "" +
                "{% block content %}{% endblock %}" +
                "{% embed '::embed.html.twig' %}\n" +
                "   {% block '<caret>' %}" +
                "{% endembed %}",
            "content"
        );
    }

    public void testThatInlineVarProvidesClassCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "{# @var bar <caret> #}", "DateTime");
    }

    public void testThatInlineVarProvidesClassCompletionDeprecated() {
        assertCompletionContains(TwigFileType.INSTANCE, "{# bar <caret> #}", "DateTime");
    }

    public void testThatConstantProvidesCompletionForClassAndDefine() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('<caret>') }}", "CONST_FOO");
    }

    public void testCompletionForRoutingParameter() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ path('xml_route', {'<caret>'}) }}", "slug");
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ path('xml_route', {'sl<caret>ug'}) }}", PlatformPatterns.psiElement());
    }

    public void testInsertHandlerForTwigFunctionWithStringParameter() {
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ a_test<caret> }}", "{{ a_test('') }}");
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ b_test<caret> }}", "{{ b_test('') }}");
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ c_test<caret> }}", "{{ c_test('') }}");
        assertCompletionResultEquals(TwigFileType.INSTANCE, "{{ d_test<caret> }}", "{{ d_test() }}");
    }

    public void testThatMacroSelfImportProvidesCompletion() {
        // skip for _self resolving issue
        if(true) return;

        assertCompletionContains(TwigFileType.INSTANCE, "" +
                "{% from _self import <caret> %}\n" +
                "{% macro foobar(foobar) %}{% endmacro %}\n",
            "foobar"
        );
    }

    public void testThatMacroImportProvidesCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "" +
                "{% import _self as foobar1 %}\n" +
                "{% macro foobar(foobar) %}{% endmacro %}\n" +
                "{{ foobar1.<caret> }}\n",
            "foobar"
        );
    }

    public void testThatIncompleteIfStatementIsCompletedWithVariables() {
        assertCompletionContains(TwigFileType.INSTANCE, "\n" +
                "{# @var \\Foo\\Template\\Foobar foobar #}\n" +
                "{% if<caret> %}\n",
            "if foobar.ready", "if foobar.readyStatus"
        );
    }

    public void testThatIncompleteForStatementIsCompletedWithVariables() {
        assertCompletionContains(TwigFileType.INSTANCE, "\n" +
                "{# @var \\Foo\\Template\\Foobar foobar #}\n" +
                "{% fo<caret> %}\n",
            "for myfoo in foobar.myfoos", "for date in foobar.dates", "for item in foobar.items"
        );
    }

    public void testThatTwigMethodStringParameterIsPipedToPhpCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "\n" +
                "{# @var \\Symfony\\Component\\HttpFoundation\\Request request #}\n" +
                "{% request.isMethod('<caret>') %}\n",
            "GET", "POST"
        );
    }

    public void testThatTwigExtensionStringParameterIsPipedToPhpCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "\n" +
                "{{ 'aaa'|request_filter('<caret>') }}\n",
            "GET", "POST"
        );

        assertCompletionContains(TwigFileType.INSTANCE, "\n" +
                "{% apply request_filter('<caret>') %}{% endapply %}\n",
            "GET", "POST"
        );

        assertCompletionContains(TwigFileType.INSTANCE, "\n" +
                "{{ request_function('<caret>') }}\n",
            "GET", "POST"
        );

        assertCompletionNotContains(TwigFileType.INSTANCE, "\n" +
                "{{ test.request_function('<caret>') }}\n",
            "GET", "POST"
        );
    }

    public void testSelfMacroImport() {
        assertCompletionContains(TwigFileType.INSTANCE, "\n" +
                "{% macro foobar(name) %}{% endmacro %}\n" +
                "{{ _self.f<caret>o }}",
            "foobar"
        );
    }

    private void createWorkaroundFile(@NotNull String file, @NotNull String content) {

        try {
            createDummyFiles(file);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // build pseudo file with block
        final VirtualFile relativeFile = VfsUtil.findRelativeFile(getProject().getBaseDir(), file.split("/"));
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                relativeFile.setBinaryContent(content.getBytes());
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            relativeFile.refresh(false, false);
        });
    }
}
