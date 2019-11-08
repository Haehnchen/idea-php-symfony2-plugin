package fr.adrienbrault.idea.symfonyplugin.tests.form;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfonyplugin.form.FormGotoCompletionRegistrar
 */
public class FormGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("foo.de.xlf");
        myFixture.copyFileToProject("messages.de.yml", "Resources/translations/messages.de.yml");
        myFixture.copyFileToProject("foo.de.yml", "Resources/translations/foo.de.yml");
        myFixture.copyFileToProject("FormOptionGotoCompletionRegistrar.php");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/form/fixtures";
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

    public void testFormChoicesTranslations() {
        String template = "<?php\n" +
            "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface\n */\n" +
            "$builder->add('foo', null, [\n" +
            "  'choices' => [\n" +
            "       '<caret>' => '',\n" +
            "   ]" +
            "]);";

        assertCompletionContains(PhpFileType.INSTANCE, template, "yaml_weak.symfony.great");
        assertNavigationMatch(PhpFileType.INSTANCE, template.replace("<caret>", "yaml_<caret>weak.symfony.great"));
    }

    public void testFormChoicesTranslationsAsChoiceTranslationDomain() {
        String template = "<?php\n" +
            "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface\n */\n" +
            "$builder->add('foo', null, [\n" +
            "  'choice_translation_domain' => 'foo',\n" +
            "  'choices' => [\n" +
            "       '<caret>' => '',\n" +
            "   ]" +
            "]);";

        assertCompletionContains(PhpFileType.INSTANCE, template, "foo.symfony.great");
        assertNavigationMatch(PhpFileType.INSTANCE, template.replace("<caret>", "foo<caret>.symfony.great"));
    }

    public void testFormChoicesTranslationsAsChoiceFormTranslationDomain() {
        String template = "<?php\n" +
            "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface\n */\n" +
            "$builder->add('foo', null, [\n" +
            "  'translation_domain' => 'foo',\n" +
            "  'choices' => [\n" +
            "       '<caret>' => '',\n" +
            "   ]" +
            "]);";

        assertCompletionContains(PhpFileType.INSTANCE, template, "foo.symfony.great");
        assertNavigationMatch(PhpFileType.INSTANCE, template.replace("<caret>", "foo<caret>.symfony.great"));
    }

    public void testFormChoicesTranslationsAsChoiceFormTranslationDomainForChoicesAsValuesIsNull() {
        String template = "<?php\n" +
            "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface\n */\n" +
            "$builder->add('foo', null, [\n" +
            "  'translation_domain' => 'foo',\n" +
            "  'choices_as_values' => false," +
            "  'choices' => [\n" +
            "       '<caret>' => '',\n" +
            "   ]" +
            "]);";

        assertCompletionNotContains(PhpFileType.INSTANCE, template, "foo.symfony.great");
    }

    public void testCompletionForChoicesAsValues() {
        String template = "<?php\n" +
            "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface\n */\n" +
            "$builder->add('foo', null, [\n" +
            "  'choices_as_values' => true," +
            "  'choices' => [\n" +
            "       '' => '<caret>',\n" +
            "   ]" +
            "]);";

        assertCompletionContains(PhpFileType.INSTANCE, template, "yaml_weak.symfony.great");
        assertNavigationMatch(PhpFileType.INSTANCE, template.replace("<caret>", "yaml_<caret>weak.symfony.great"));
    }

    public void testCompletionForChoicesAsValuesForSymfonyVersions() {
        initVersion("2.7");

        String template = "<?php\n" +
            "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface\n */\n" +
            "$builder->add('foo', null, [\n" +
            "  'choices' => [\n" +
            "       '' => '<caret>',\n" +
            "   ]" +
            "]);";

        assertCompletionContains("foo.php", template, "yaml_weak.symfony.great");
        assertNavigationMatch("foo.php", template.replace("<caret>", "yaml_<caret>weak.symfony.great"));
    }

    public void testCompletionForChoicesAsValuesForSymfonyVersions2() {
        initVersion("2.8");

        String template = "<?php\n" +
            "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface\n */\n" +
            "$builder->add('foo', null, [\n" +
            "  'choices' => [\n" +
            "       '<caret>' => '',\n" +
            "   ]" +
            "]);";

        assertCompletionContains("foo.php", template, "yaml_weak.symfony.great");
        assertNavigationMatch("foo.php", template.replace("<caret>", "yaml_<caret>weak.symfony.great"));

        template = "<?php\n" +
            "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface\n */\n" +
            "$builder->add('foo', null, [\n" +
            "  'choices' => [\n" +
            "       '' => '<caret>',\n" +
            "   ]" +
            "]);";

        assertCompletionNotContains("foo.php", template, "yaml_weak.symfony.great");
    }

    public void testFormChoicesTranslationsAsChoiceTranslationDomainInsideDefaults() {
        String template = "<?php\n" +
            "\n" +
            "use Symfony\\Component\\Form\\AbstractType;\n" +
            "use Symfony\\Component\\Form\\FormBuilderInterface;\n" +
            "use Symfony\\Component\\OptionsResolver\\OptionsResolver;\n" +
            "\n" +
            "class FooType extends AbstractType\n" +
            "{\n" +
            "    public function buildForm(FormBuilderInterface $builder, array $options)\n" +
            "    {\n" +
            "        $builder->add('foobar', null, [\n" +
            "            'choices' => [\n" +
            "                '<caret>' => '',\n" +
            "            ]\n" +
            "        ]);\n" +
            "    }\n" +
            "\n" +
            "    public function configureOptions(OptionsResolver $resolver)\n" +
            "    {\n" +
            "        $resolver->setDefaults([\n" +
            "            'translation_domain' => 'foo',\n" +
            "        ]);\n" +
            "    }\n" +
            "}\n";

        assertCompletionContains(PhpFileType.INSTANCE, template, "foo.symfony.great");
        assertNavigationMatch(PhpFileType.INSTANCE, template.replace("<caret>", "foo.symfony<caret>.great"));
    }

    private void initVersion(@NotNull String version) {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\HttpKernel {\n" +
            "   class Kernel {\n" +
            "       const VERSION = '" + version + "';" +
            "   }" +
            "}"
        );
    }
}
