package fr.adrienbrault.idea.symfony2plugin.tests.form;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.form.FormOptionGotoCompletionRegistrar
 */
public class FormOptionGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("FormOptionGotoCompletionRegistrar.php");
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testFormReferenceCompletionProvider() {
        String[] types = {
            "'\\Foo\\Form\\Bar'",
            "\\Foo\\Form\\Bar::class",
            "new \\Foo\\Form\\Bar()"
        };

        for (String s : types) {
            assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                    String.format("$builder->add('foo', %s, [\n", s) +
                    "'<caret>'\n" +
                    "])",
                "configure_options"
            );

            assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                    String.format("$builder->add('foo', %s, [\n", s) +
                    "'<caret>' => ''\n" +
                    "])",
                "configure_options"
            );

            assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                    String.format("$builder->add('foo', %s, [\n", s) +
                    "'configure_options<caret>'\n" +
                    "])",
                PlatformPatterns.psiElement()
            );

            assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                    String.format("$builder->add('foo', %s, [\n", s) +
                    "'configure_options<caret>' => null\n" +
                    "])",
                PlatformPatterns.psiElement()
            );
        }
    }
}
