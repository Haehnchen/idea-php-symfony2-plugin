package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.TwigBlockTag;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateGoToDeclarationHandler
 */
public class TwigTemplateGoToDeclarationHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     */
    public void testBlockNavigation() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        createWorkaroundFile("app/Resources/views/block.html.twig", "{% block foo %}{% endblock %}");

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

        createWorkaroundFile("app/Resources/views/embed.html.twig", "{% block foo_embed %}{% endblock %}");

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
