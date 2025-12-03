package fr.adrienbrault.idea.symfony2plugin.tests.codeInspection.command;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.codeInspection.command.CommandInvokableReturnTypeInspection
 */
public class CommandInvokableReturnTypeInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/codeInspection/command/fixtures";
    }

    public void testCommandWithoutIntReturnType() {
        assertLocalInspectionContains("test.php", "<?php\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "\n" +
            "#[AsCommand(name: 'app:foobar')]\n" +
            "class FoobarCommand\n" +
            "{\n" +
            "    public function __inv<caret>oke(): void\n" +
            "    {\n" +
            "    }\n" +
            "}",
            "Symfony: Consider adding int return type to command __invoke()"
        );
    }

    public void testCommandWithStringReturnType() {
        assertLocalInspectionContains("test.php", "<?php\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "\n" +
            "#[AsCommand(name: 'app:foobar')]\n" +
            "class FoobarCommand\n" +
            "{\n" +
            "    public function __inv<caret>oke(): string\n" +
            "    {\n" +
            "        return 'test';\n" +
            "    }\n" +
            "}",
            "Symfony: Consider adding int return type to command __invoke()"
        );
    }

    public void testCommandWithNoReturnType() {
        assertLocalInspectionContains("test.php", "<?php\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "\n" +
            "#[AsCommand(name: 'app:foobar')]\n" +
            "class FoobarCommand\n" +
            "{\n" +
            "    public function __inv<caret>oke()\n" +
            "    {\n" +
            "    }\n" +
            "}",
            "Symfony: Consider adding int return type to command __invoke()"
        );
    }

    public void testCommandWithIntReturnType() {
        assertLocalInspectionNotContains("test.php", "<?php\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "\n" +
            "#[AsCommand(name: 'app:foobar')]\n" +
            "class FoobarCommand\n" +
            "{\n" +
            "    public function __inv<caret>oke(): int\n" +
            "    {\n" +
            "        return 0;\n" +
            "    }\n" +
            "}",
            "Symfony: Consider adding int return type to command __invoke()"
        );
    }

    public void testNonCommandClassIsIgnored() {
        assertLocalInspectionNotContains("test.php", "<?php\n" +
            "class FoobarClass\n" +
            "{\n" +
            "    public function __inv<caret>oke(): void\n" +
            "    {\n" +
            "    }\n" +
            "}",
            "Symfony: Consider adding int return type to command __invoke()"
        );
    }

    public void testTraditionalCommandWithoutAttributeIsIgnored() {
        assertLocalInspectionNotContains("test.php", "<?php\n" +
            "use Symfony\\Component\\Console\\Command\\Command;\n" +
            "\n" +
            "class FoobarCommand extends Command\n" +
            "{\n" +
            "    public function __inv<caret>oke(): void\n" +
            "    {\n" +
            "    }\n" +
            "}",
            "Symfony: Consider adding int return type to command __invoke()"
        );
    }
}
