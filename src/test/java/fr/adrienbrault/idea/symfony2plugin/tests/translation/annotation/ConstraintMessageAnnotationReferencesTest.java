package fr.adrienbrault.idea.symfony2plugin.tests.translation.annotation;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.translation.annotation.ConstraintMessageAnnotationReferences;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ConstraintMessageAnnotationReferences
 */
public class ConstraintMessageAnnotationReferencesTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("validators.de.yml", "Resources/translations/validators.de.yml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/annotation/fixtures";
    }

    public void testThatCompletionIsGivenForConstraintMessageInAnnotation() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
            "\n" +
            "use Symfony\\Component\\Validator\\Constraints as Assert;\n" +
            "\n" +
            "/**\n" +
            " * @Assert\\Email(message=\"symfony<caret>\")\n" +
            " */\n" +
            "class Foo\n" +
            "{\n" +
            "}",
            "symfony_great"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "use Symfony\\Component\\Validator\\Constraints as Assert;\n" +
                "\n" +
                "/**\n" +
                " * @Assert\\Email(messages=\"symfony<caret>\")\n" +
                " */\n" +
                "class Foo\n" +
                "{\n" +
                "}",
            "symfony_great"
        );
    }

    public void testThatNavigationIsGivenForConstraintMessageInAnnotation() {
        assertReferenceMatchOnParent(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "use Symfony\\Component\\Validator\\Constraints as Assert;\n" +
                "\n" +
                "/**\n" +
                " * @Assert\\Email(message=\"symfony<caret>_great\")\n" +
                " */\n" +
                "class Foo {}\n",
            PlatformPatterns.psiElement()
        );

        assertReferenceMatchOnParent(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "use Symfony\\Component\\Validator\\Constraints as Assert;\n" +
                "\n" +
                "/**\n" +
                " * @Assert\\Email(messages=\"symfony<caret>_great\")\n" +
                " */\n" +
                "class Foo {}\n",
            PlatformPatterns.psiElement()
        );
    }
}

