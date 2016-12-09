package fr.adrienbrault.idea.symfony2plugin.tests.dic.linemarker;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.linemarker.YamlLineMarkerProvider
 */
public class YamlLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.xml"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testThatDecoratedServiceShouldProvideMarker() {
        assertLineMarker(createYamlFile("" +
                "services:\n" +
                "\n" +
                "    foo_bar_main:\n" +
                "        class: Foo\\Bar\n"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to decoration")
        );
    }

    public void testThatDecoratesProvidesOverwriteMarker() {
        assertLineMarker(createYamlFile("" +
                "services:\n" +
                "\n" +
                "    foo_bar_main:\n" +
                "        decorates: app.mailer\n"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to decorated service")
        );
    }

    @NotNull
    private PsiElement createYamlFile(@NotNull String content) {
        return PsiFileFactory.getInstance(getProject()).createFileFromText("DUMMY__." + YAMLFileType.YML.getDefaultExtension(), YAMLFileType.YML, content);
    }
}
