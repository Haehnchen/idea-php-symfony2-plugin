package fr.adrienbrault.idea.symfony2plugin.tests.form;

import com.jetbrains.php.lang.PhpFileType;
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
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("foo.de.xlf"));
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
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
            }
        }
    }
}
