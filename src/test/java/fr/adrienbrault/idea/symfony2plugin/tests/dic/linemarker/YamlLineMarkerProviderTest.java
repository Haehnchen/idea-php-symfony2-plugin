package fr.adrienbrault.idea.symfony2plugin.tests.dic.linemarker;

import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLKeyValue;

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
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/linemarker/fixtures";
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
        myFixture.addFileToProject("config/services_parent.yaml", "" +
            "services:\n" +
            "    app.child:\n" +
            "        parent: foo_bar_parent_main\n");

        PsiElement yamlFile = createYamlFile("" +
            "services:\n" +
            "\n" +
            "    foo_bar_parent_main:\n" +
            "        class: Foo\\Bar\n"
        );

        assertLineMarker(yamlFile, new LineMarker.ToolTipEqualsAssert("Navigate to parent"));
        assertLineMarker(yamlFile, new LineMarker.TargetAcceptsPattern("Navigate to parent",
            PlatformPatterns.psiElement(YAMLKeyValue.class).with(new PatternCondition<>("KeyText") {
                @Override
                public boolean accepts(@NotNull YAMLKeyValue yamlKeyValue, ProcessingContext context) {
                    return "app.child".equals(yamlKeyValue.getKeyText());
                }
            }))
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

    public void testThatServiceResourceIsHavingLinemarker() {
        myFixture.addFileToProject("src/Controller/FooController.php", "<?php\n" +
            "namespace App\\Controller;\n" +
            "class FooController {}\n");

        PsiFile configFile = myFixture.addFileToProject("config/services.yaml",
            "services:\n" +
            "    App\\Controller\\:\n" +
            "        resource: '../src/Controller/*'\n");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to class"));
        assertLineMarker(myFixture.getFile(), new LineMarker.TargetAcceptsPattern("Navigate to class",
            PlatformPatterns.psiElement(PhpClass.class).with(new PatternCondition<>("fqn") {
                @Override
                public boolean accepts(@NotNull PhpClass phpClass, ProcessingContext context) {
                    return "\\App\\Controller\\FooController".equals(phpClass.getFQN());
                }
            }))
        );
    }

    @NotNull
    private PsiElement createYamlFile(@NotNull String content) {
        return PsiFileFactory.getInstance(getProject()).createFileFromText("DUMMY__." + YAMLFileType.YML.getDefaultExtension(), YAMLFileType.YML, content);
    }
}
