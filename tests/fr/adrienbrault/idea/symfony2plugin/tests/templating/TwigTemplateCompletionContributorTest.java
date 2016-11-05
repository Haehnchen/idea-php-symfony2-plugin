package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
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
        assertCompletionContains(TwigFileType.INSTANCE, "{# @var bar Date<caret> #}", "DateTime");
    }

    public void testThatInlineVarProvidesClassCompletionDeprecated() {
        assertCompletionContains(TwigFileType.INSTANCE, "{# bar Date<caret> #}", "DateTime");
    }

    public void testThatConstantProvidesCompletionForClassAndDefine() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ constant('<caret>') }}", "CONST_FOO");
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
