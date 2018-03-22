package fr.adrienbrault.idea.symfony2plugin.tests.form.completion;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.form.completion.FormCompletionContributor
 */
public class FormCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testClassConstantsCompletionWithoutNamespace() {
        assertCompletionResultEquals(PhpFileType.INSTANCE,
            "<?php\n /** @var $foo \\Symfony\\Component\\Form\\FormBuilderInterface */\n $foo->add('', HiddenType<caret>)",
            "<?php\n /** @var $foo \\Symfony\\Component\\Form\\FormBuilderInterface */\n $foo->add('', \\Symfony\\Component\\Form\\Extension\\Core\\Type\\HiddenType::class)",
            new LookupElementInsert.Icon(Symfony2Icons.FORM_TYPE)
        );
    }

    public void testClassConstantsCompletionWithNamespaceShouldInsertUseAndStripNamespace() {
        assertCompletionResultEquals(PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Foo {\n" +
                "  /** @var $foo \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                "  $foo->add('', HiddenType<caret>);\n" +
                "};",
            "<?php\n" +
                "namespace Foo {\n\n" +
                "    use Symfony\\Component\\Form\\Extension\\Core\\Type\\HiddenType;\n\n" +
                "    /** @var $foo \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                "  $foo->add('', HiddenType::class);\n" +
                "};",
            new LookupElementInsert.Icon(Symfony2Icons.FORM_TYPE)
        );
    }

}
