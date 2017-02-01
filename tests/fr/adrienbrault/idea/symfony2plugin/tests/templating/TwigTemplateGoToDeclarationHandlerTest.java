package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.TwigBlockTag;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateGoToDeclarationHandler
 */
public class TwigTemplateGoToDeclarationHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {
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
