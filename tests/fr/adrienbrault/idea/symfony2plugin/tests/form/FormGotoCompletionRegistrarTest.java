package fr.adrienbrault.idea.symfony2plugin.tests.form;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.form.FormGotoCompletionRegistrar
 */
public class FormGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("foo.de.xlf");
        myFixture.copyFileToProject("services.xml");
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testFormTypeCompletion() {
        for (String clazz : new String[] {"Symfony\\Component\\Form\\FormBuilderInterface", "Symfony\\Component\\Form\\FormInterface"}) {
            for (String method : new String[]{"add", "create"}) {
                assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                        String.format("/** @var $builder \\%s */\n", clazz) +
                        String.format("$builder->%s('foo', '<caret>');", method),
                    "foo_type"
                );

                assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                        String.format("/** @var $builder \\%s */\n", clazz) +
                        String.format("$builder->%s('foo', 'foo_type<caret>');", method),
                    PlatformPatterns.psiElement(PhpClass.class).withName("Foo")
                );
            }
        }
    }

    public void testFormOptionTranslationDomain() {

        for (String clazz : new String[] {"Symfony\\Component\\Form\\FormBuilderInterface", "Symfony\\Component\\Form\\FormInterface"}) {
            for (String method : new String[] {"add", "create"}) {

                assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                        String.format("/** @var $builder \\%s */\n", clazz) +
                        String.format("$builder->%s('foo', null, [\n", method) +
                        "  'choice_translation_domain' => '<caret>'\n" +
                        "]);",
                    "foo"
                );

                assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                        String.format("/** @var $builder \\%s */\n", clazz) +
                        String.format("$builder->%s('foo', null, [\n", method) +
                        "  'translation_domain' => '<caret>'\n" +
                        "]);",
                    "foo"
                );

                assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                        String.format("/** @var $builder \\%s */\n", clazz) +
                        String.format("$builder->%s('foo', null, [\n", method) +
                        "  'choice_translation_domain' => 'fo<caret>o'\n" +
                        "]);",
                    PlatformPatterns.psiFile().withName("foo.de.xlf")
                );

                assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                        String.format("/** @var $builder \\%s */\n", clazz) +
                        String.format("$builder->%s('foo', null, [\n", method) +
                        "  'translation_domain' => 'fo<caret>o'\n" +
                        "]);",
                    PlatformPatterns.psiFile().withName("foo.de.xlf")
                );
            }
        }
    }
}
