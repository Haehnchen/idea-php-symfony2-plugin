package fr.adrienbrault.idea.symfonyplugin.tests.dic.linemarker;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.dic.linemarker.YamlLineMarkerProvider
 */
public class YamlLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.xml"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/dic/linemarker/fixtures";
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

    public void testThatParentServiceShouldProvideMarker() {
        assertLineMarker(createYamlFile("" +
                "services:\n" +
                "\n" +
                "    foo_bar_main:\n" +
                "        class: Foo\\Bar\n"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to parent")
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

    public void testThatParentProvidesOverwriteMarker() {
        assertLineMarker(createYamlFile("" +
                "services:\n" +
                "\n" +
                "    foo_bar_main:\n" +
                "        parent: app.mailer\n"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to parent service")
        );
    }

    @NotNull
    private PsiElement createYamlFile(@NotNull String content) {
        return PsiFileFactory.getInstance(getProject()).createFileFromText("DUMMY__." + YAMLFileType.YML.getDefaultExtension(), YAMLFileType.YML, content);
    }
}
