package fr.adrienbrault.idea.symfony2plugin.tests.form;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.form.FormTypeReferenceContributor
 */
public class FormTypeReferenceContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testDataClassPropertyCompletion() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "class FormType\n" +
                "{\n" +
                "    public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options) {\n" +
                "        $builder->add('<caret>');\n" +
                "    }\n" +
                "    public function setDefaultOptions(OptionsResolverInterface $resolver)\n" +
                "    {\n" +
                "        $resolver->setDefaults(array(\n" +
                "            'data_class' => \"DateTime\",\n" +
                "        ));\n" +
                "    }\n" +
                "}",
            "timestamp"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "class FormType\n" +
                "{\n" +
                "    public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options) {\n" +
                "        $builder->add('<caret>');\n" +
                "    }\n" +
                "    public function setDefaultOptions(OptionsResolverInterface $resolver)\n" +
                "    {\n" +
                "        $resolver->setDefaults(array(\n" +
                "            'data_class' => DateTime::class,\n" +
                "        ));\n" +
                "    }\n" +
                "}",
            "timestamp"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "class FormType\n" +
                "{\n" +
                "    protected $foo = 'DateTime';\n" +
                "    public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options) {\n" +
                "        $builder->add('<caret>');\n" +
                "    }\n" +
                "    public function setDefaultOptions(OptionsResolverInterface $resolver)\n" +
                "    {\n" +
                "        $resolver->setDefaults(array(\n" +
                "            'data_class' => $this->foo,\n" +
                "        ));\n" +
                "    }\n" +
                "}",
            "timestamp"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "class FormType\n" +
                "{\n" +
                "    protected $foo = 'DateTime';\n" +
                "    public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options) {\n" +
                "        $builder->add('<caret>');\n" +
                "    }\n" +
                "    public function setDefaultOptions(OptionsResolverInterface $resolver)\n" +
                "    {\n" +
                "        $resolver->setDefaults(array(\n" +
                "            'data_class' => \\Form\\DataClass\\Model::class,\n" +
                "        ));\n" +
                "    }\n" +
                "}",
            "fooBar"
        );
    }

    public void testDataClassWithPublicPropertyCompletion() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "class FormType\n" +
                "{\n" +
                "    protected $foo = 'DateTime';\n" +
                "    public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options) {\n" +
                "        $builder->add('<caret>');\n" +
                "    }\n" +
                "    public function setDefaultOptions(OptionsResolverInterface $resolver)\n" +
                "    {\n" +
                "        $resolver->setDefaults(array(\n" +
                "            'data_class' => \\Form\\DataClass\\Model::class,\n" +
                "        ));\n" +
                "    }\n" +
                "}",
            "var", "varBar"
        );
    }

    public void testFormDataFieldPropertyPathReferencesForMethod() {
        for (String s : new String[]{"foo_bar", "fooBar"}) {
            assertReferenceMatchOnParent(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "class FormType\n" +
                "{\n" +
                "    protected $foo = 'DateTime';\n" +
                "    public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options) {\n" +
                "        $builder->add('" + s + "<caret>');\n" +
                "    }\n" +
                "    public function setDefaultOptions(OptionsResolverInterface $resolver)\n" +
                "    {\n" +
                "        $resolver->setDefaults(array(\n" +
                "            'data_class' => \\Form\\DataClass\\Model::class,\n" +
                "        ));\n" +
                "    }\n" +
                "}", PlatformPatterns.psiElement(Method.class)
            );
        }
    }

    public void testFormDataFieldPropertyPathReferencesForProperty() {
        for (String s : new String[]{"var", "varBar", "var_bar", "var_Bar"}) {
            assertReferenceMatchOnParent(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "class FormType\n" +
                "{\n" +
                "    protected $foo = 'DateTime';\n" +
                "    public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options) {\n" +
                "        $builder->add('" + s + "<caret>');\n" +
                "    }\n" +
                "    public function setDefaultOptions(OptionsResolverInterface $resolver)\n" +
                "    {\n" +
                "        $resolver->setDefaults(array(\n" +
                "            'data_class' => \\Form\\DataClass\\Model::class,\n" +
                "        ));\n" +
                "    }\n" +
                "}", PlatformPatterns.psiElement(Field.class)
            );
        }
    }
}
