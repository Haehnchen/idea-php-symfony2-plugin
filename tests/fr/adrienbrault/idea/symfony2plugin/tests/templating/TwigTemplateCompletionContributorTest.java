package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.IOException;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see com.jetbrains.twig.completion.TwigCompletionContributor
 */
public class TwigTemplateCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     */
    public void testBlockCompletion() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        try {
            createDummyFiles(
                "app/Resources/views/block.html.twig"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        // build pseudo file with block
        final VirtualFile relativeFile = VfsUtil.findRelativeFile(getProject().getBaseDir(), "app/Resources/views/block.html.twig".split("/"));
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    relativeFile.setBinaryContent("{% block foo %}{% endblock %}".getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                relativeFile.refresh(false, false);
            }
        });

        assertCompletionContains(TwigFileType.INSTANCE, "{% extends '::block.html.twig' %}{% block <caret> %}", "foo");
        assertCompletionContains(TwigFileType.INSTANCE, "{% extends '::block.html.twig' %}{% block \"<caret>\" %}", "foo");
        assertCompletionContains(TwigFileType.INSTANCE, "{% extends '::block.html.twig' %}{% block '<caret>' %}", "foo");
    }

    public void testThatInlineVarProvidesClassCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "{# @var bar Date<caret> #}", "DateTime");
    }

    public void testThatInlineVarProvidesClassCompletionDeprecated() {
        assertCompletionContains(TwigFileType.INSTANCE, "{# bar Date<caret> #}", "DateTime");
    }
}
