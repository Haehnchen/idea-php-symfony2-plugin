package fr.adrienbrault.idea.symfony2plugin.tests.translation;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.translation.ConstraintMessageGotoCompletionRegistrar
 */
public class ConstraintMessageGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("ConstraintMessageGotoCompletionRegistrar.php");
        myFixture.copyFileToProject("validators.de.yml", "Resources/translations/validators.de.yml");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/fixtures";
    }

    public void testThatPropertyStartingWithMessageInsideConstraintImplementationProvidesMessageCompletion() {
        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "class FooConstraint extends \\Symfony\\Component\\Validator\\Constraint\n" +
                "{\n" +
                "    public $message = '<caret>';\n" +
                "}",
            "validator_message"
        );
    }

    public void testThatAnnotationMessageProvidesMessageCompletion() {
        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\Validator\\Constraints {\n" +
                "    class Email extends \\Symfony\\Component\\Validator\\Constraint {}\n" +
                "}\n" +
                "\n" +
                "namespace {\n" +
                "    use Symfony\\Component\\Validator\\Constraints as Assert;\n" +
                "\n" +
                "    /**\n" +
                "     * @Assert\\Email(message=\"validator<caret>\")\n" +
                "     */\n" +
                "    class Foo {}\n" +
                "}\n",
            "validator_message"
        );

        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\Validator\\Constraints {\n" +
                "    class Email extends \\Symfony\\Component\\Validator\\Constraint {}\n" +
                "}\n" +
                "\n" +
                "namespace {\n" +
                "    use Symfony\\Component\\Validator\\Constraints as Assert;\n" +
                "\n" +
                "    /**\n" +
                "     * @Assert\\Email(messages=\"validator<caret>\")\n" +
                "     */\n" +
                "    class Foo {}\n" +
                "}\n",
            "validator_message"
        );
    }

    public void testThatPhpAttributeMessageProvidesMessageCompletion() {
        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "use Symfony\\Component\\Validator\\Constraints\\NotBlank;\n" +
                "\n" +
                "#[NotBlank(message: '<caret>')]\n" +
                "class Foo {}\n",
            "validator_message"
        );

        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "use Symfony\\Component\\Validator\\Constraints\\Length;\n" +
                "\n" +
                "#[Length(minMessage: '<caret>')]\n" +
                "class Foo {}\n",
            "validator_message"
        );

        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "use Symfony\\Component\\Validator\\Constraints as Assert;\n" +
                "\n" +
                "#[Assert\\NotBlank(message: '<caret>')]\n" +
                "class Foo {}\n",
            "validator_message"
        );
    }

    public void testThatPhpAttributeOptionsArrayMessageProvidesMessageCompletionAndNavigation() {
        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "use Symfony\\Component\\Validator\\Constraints\\NotBlank;\n" +
                "\n" +
                "#[NotBlank(['message' => '<caret>'])]\n" +
                "class Foo {}\n",
            "validator_message"
        );

        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "use Symfony\\Component\\Validator\\Constraints as Assert;\n" +
                "\n" +
                "#[Assert\\NotBlank(['message' => '<caret>'])]\n" +
                "class Foo {}\n",
            "validator_message"
        );

        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "use Symfony\\Component\\Validator\\Constraints\\Length;\n" +
                "\n" +
                "#[Length(['minMessage' => '<caret>'])]\n" +
                "class Foo {}\n",
            "validator_message"
        );

        assertNavigationMatch(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "use Symfony\\Component\\Validator\\Constraints\\NotBlank;\n" +
                "\n" +
                "#[NotBlank(['message' => 'validator<caret>_message'])]\n" +
                "class Foo {}\n",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "use Symfony\\Component\\Validator\\Constraints as Assert;\n" +
                "\n" +
                "#[Assert\\NotBlank(['message' => 'validator<caret>_message'])]\n" +
                "class Foo {}\n",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "use Symfony\\Component\\Validator\\Constraints\\Length;\n" +
                "\n" +
                "#[Length(['minMessage' => 'validator<caret>_message'])]\n" +
                "class Foo {}\n",
            PlatformPatterns.psiElement()
        );
    }

    public void testThatPropertyStartingWithMessageInsideConstraintImplementationProvidesMessageNavigation() {
        assertNavigationMatch(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "class FooConstraint extends \\Symfony\\Component\\Validator\\Constraint\n" +
                "{\n" +
                "    public $message = 'validato<caret>r_message';\n" +
                "}",
            PlatformPatterns.psiElement()
        );
    }

    public void testThatAnnotationMessageProvidesMessageNavigation() {
        assertNavigationMatch(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\Validator\\Constraints {\n" +
                "    class Email extends \\Symfony\\Component\\Validator\\Constraint {}\n" +
                "}\n" +
                "\n" +
                "namespace {\n" +
                "    use Symfony\\Component\\Validator\\Constraints as Assert;\n" +
                "\n" +
                "    /**\n" +
                "     * @Assert\\Email(message=\"validator<caret>_message\")\n" +
                "     */\n" +
                "    class Foo {}\n" +
                "}\n",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\Validator\\Constraints {\n" +
                "    class Email extends \\Symfony\\Component\\Validator\\Constraint {}\n" +
                "}\n" +
                "\n" +
                "namespace {\n" +
                "    use Symfony\\Component\\Validator\\Constraints as Assert;\n" +
                "\n" +
                "    /**\n" +
                "     * @Assert\\Email(messages=\"validator<caret>_message\")\n" +
                "     */\n" +
                "    class Foo {}\n" +
                "}\n",
            PlatformPatterns.psiElement()
        );
    }

    public void testThatPhpAttributeOptionsArrayMessageNavigationIsNotGivenForNonConstraintAttribute() {
        assertNavigationIsEmpty(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App {\n" +
            "    class NotBlank { public $message; }\n" +
            "}\n" +
            "\n" +
            "namespace {\n" +
            "    use App\\NotBlank;\n" +
            "\n" +
            "    #[NotBlank(['message' => 'validator<caret>_message'])]\n" +
            "    class Foo {}\n" +
            "}\n"
        );
    }

    public void testThatAnnotationMessageNavigationIsNotGivenForNonConstraintAnnotation() {
        assertNavigationIsEmpty(PhpFileType.INSTANCE, "<?php\n" +
            "class Email\n" +
            "{\n" +
            "}\n" +
            "\n" +
            "/**\n" +
            " * @Email(message=\"validator<caret>_message\")\n" +
            " */\n" +
            "class Foo {}\n"
        );
    }
}
