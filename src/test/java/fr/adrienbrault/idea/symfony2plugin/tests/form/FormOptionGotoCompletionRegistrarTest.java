package fr.adrienbrault.idea.symfony2plugin.tests.form;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

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
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/form/fixtures";
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
                "configure_options", "class_const_option", "global_const_option", "global_const_define"
            );

            assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                    String.format("$builder->add('foo', %s, [\n", s) +
                    "'<caret>' => ''\n" +
                    "])",
                "configure_options", "class_const_option", "global_const_option", "global_const_define"
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

    public void testFormReferenceCompletionProviderForDefaultOptionsParameter() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $resolver \\Symfony\\Component\\OptionsResolver\\OptionsResolver */\n" +
                "$resolver->setDefault('<caret>')",
            "configure_options"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $resolver \\Symfony\\Component\\OptionsResolver\\OptionsResolver */\n" +
                "$resolver->setDefault('configure_<caret>options')",
            PlatformPatterns.psiElement()
        );
    }

    public void testGuessedOptionsCompleteAndNavigate() {
        myFixture.addFileToProject("IntegerType.php", """
            <?php
            namespace Symfony\\Component\\Form\\Extension\\Core\\Type;

            class IntegerType extends \\Symfony\\Component\\Form\\AbstractType {
                public function configureOptions($resolver) {
                    $resolver->setDefaults(['grouping' => false]);
                }
            }
            """);
        myFixture.addFileToProject("EventData.php", "<?php class EventData { public ?int $capacity; }");

        String code = """
            <?php
            class EventType extends \\Symfony\\Component\\Form\\AbstractType {
                public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options) {
                    $builder->add(%s);
                }

                public function configureOptions(\\Symfony\\Component\\OptionsResolver\\OptionsResolver $resolver) {
                    $resolver->setDefaults(['data_class' => EventData::class]);
                }
            }
            """;

        assertCompletionContains(
            PhpFileType.INSTANCE, code.formatted("'capacity', null, ['<caret>']"), "grouping"
        );
        assertCompletionContains(
            PhpFileType.INSTANCE, code.formatted("options: ['<caret>'], child: 'capacity'"), "grouping"
        );
        assertNavigationMatch(
            PhpFileType.INSTANCE, code.formatted("'capacity', null, ['group<caret>ing' => true]"),
            PlatformPatterns.psiElement(StringLiteralExpression.class).withText("'grouping'")
        );
    }
}
