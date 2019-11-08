package fr.adrienbrault.idea.symfony2plugin.tests.form;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil#getDefaultOptions
 */
public class FormArrayOptionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/form/fixtures";
    }

    public void testFormOptionCompletion() {

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\r\n" +
                "public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options) {\n" +
                "   $builder->add('foo', 'form', array('<caret>'));\n" +
                "}\n",
            "configure_options", "default_options"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\r\n" +
                "public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options) {\n" +
                "   $builder->add('foo', 'form', array('foo' => 'foo', '<caret>'));\n" +
                "}\n",
            "configure_options", "default_options"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\r\n" +
                "public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options) {\n" +
                "   $builder->add('foo', 'form', array('foo' => 'foo', '<caret>' => 'foo'));\n" +
                "}\n",
            "configure_options", "default_options"
        );
    }

    public void testFormArrayValuesCompletion() {

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\r\n" +
                "public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options) {\n" +
                "   $builder->add('foo', 'form', array('<caret>'));\n" +
                "}\n",
            "default_setDefined", "default_setOptional", "default_setRequired", "default_setRequired2"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\r\n" +
                "public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options) {\n" +
                "   $builder->add('foo', 'form', array('<caret>'));\n" +
                "}\n",
            "configure_setDefined", "configure_setOptional", "configure_setRequired", "configure_setRequired2"
        );

    }

    public void testThatUnknownTypeProvidesFormFallback() {

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\r\n" +
                "public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options) {\n" +
                "   $builder->add('foo', 'ops_unknown', array('<caret>'));\n" +
                "}\n",
            "configure_options", "default_options"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\r\n" +
                "public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options) {\n" +
                "   $builder->add('foo', null, array('<caret>'));\n" +
                "}\n",
            "configure_options", "default_options"
        );

    }


}
