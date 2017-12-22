package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
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
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     */
    public void testBlockCompletion() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        createWorkaroundFile("app/Resources/views/block.html.twig", "{% block foo %}{% endblock %}");

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

        createWorkaroundFile("app/Resources/views/embed.html.twig", "{% block foo_embed %}{% endblock %}");

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
