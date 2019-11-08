package fr.adrienbrault.idea.symfonyplugin.tests.templating;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.templating.TwigLineMarkerProvider
 */
public class TwigLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testThatExtendsProvidesBlockLineMarker() {
        if(System.getenv("PHPSTORM_ENV") != null) return;
        myFixture.addFileToProject("app/Resources/views/block.html.twig", "{% block foo %}{% endblock %}");

        PsiFile psiFile = myFixture.configureByText(TwigFileType.INSTANCE, "" +
            "{% extends 'block.html.twig' %}\n" +
            "{% block foo %}{% endblock %}"
        );

        assertLineMarker(psiFile, markerInfo -> "foo".equals(markerInfo.getElement().getText()));
    }

    public void testThatBlockInsideEmbedMustProvideBlockLineMarker() {
        if(System.getenv("PHPSTORM_ENV") != null) return;
        myFixture.addFileToProject("app/Resources/views/block.html.twig", "{% block foo %}{% endblock %}");

        PsiFile psiFile = myFixture.configureByText(TwigFileType.INSTANCE, "" +
            "{% embed 'block.html.twig' %}\n" +
            "   {% block foo %}{% endblock %}\n" +
            "{% endembed %}"
        );

        assertLineMarker(psiFile, markerInfo -> "foo".equals(markerInfo.getElement().getText()));
    }

    public void testThatBlockInsideEmbedMustNotProvideBlockLineMarkerForFileScope() {
        if(System.getenv("PHPSTORM_ENV") != null) return;
        myFixture.addFileToProject("app/Resources/views/block.html.twig", "{% block foo %}{% endblock %}");

        PsiFile psiFile = myFixture.configureByText(TwigFileType.INSTANCE, "" +
            "{% extends 'block.html.twig' %}\n" +
            "{% embed 'foo.html.twig' %}\n" +
            "   {% block foo %}{% endblock %}\n" +
            "{% endembed %}"
        );

        assertLineMarkerIsEmpty(psiFile);
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
