package fr.adrienbrault.idea.symfony2plugin.tests.codeInspection.command;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.codeInspection.command.CommandInvokableReturnValueInspection
 */
public class CommandInvokableReturnValueInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/codeInspection/command/fixtures";
    }

    public void testCommandReturningString() {
        assertLocalInspectionContains("test.php", "<?php\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "\n" +
            "#[AsCommand(name: 'app:foobar')]\n" +
            "class FoobarCommand\n" +
            "{\n" +
            "    public function __invoke()\n" +
            "    {\n" +
            "        return '<caret>test';\n" +
            "    }\n" +
            "}",
            "Symfony: Command must return an integer value"
        );
    }

    public void testCommandReturningNull() {
        assertLocalInspectionContains("test.php", "<?php\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "\n" +
            "#[AsCommand(name: 'app:foobar')]\n" +
            "class FoobarCommand\n" +
            "{\n" +
            "    public function __invoke()\n" +
            "    {\n" +
            "        return n<caret>ull;\n" +
            "    }\n" +
            "}",
            "Symfony: Command must return an integer value"
        );
    }

    public void testCommandReturningIntegerLiteral() {
        assertLocalInspectionNotContains("test.php", "<?php\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "\n" +
            "#[AsCommand(name: 'app:foobar')]\n" +
            "class FoobarCommand\n" +
            "{\n" +
            "    public function __invoke()\n" +
            "    {\n" +
            "        return <caret>0;\n" +
            "    }\n" +
            "}",
            "Symfony: Command must return an integer value"
        );
    }

    public void testCommandReturningIntegerVariable() {
        assertLocalInspectionNotContains("test.php", "<?php\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "\n" +
            "#[AsCommand(name: 'app:foobar')]\n" +
            "class FoobarCommand\n" +
            "{\n" +
            "    public function __invoke()\n" +
            "    {\n" +
            "        $exitCode = 0;\n" +
            "        return $exit<caret>Code;\n" +
            "    }\n" +
            "}",
            "Symfony: Command must return an integer value"
        );
    }

    public void testCommandReturningCommandSuccess() {
        assertLocalInspectionNotContains("test.php", "<?php\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "use Symfony\\Component\\Console\\Command\\Command;\n" +
            "\n" +
            "#[AsCommand(name: 'app:foobar')]\n" +
            "class FoobarCommand\n" +
            "{\n" +
            "    public function __invoke()\n" +
            "    {\n" +
            "        return Command::SUC<caret>CESS;\n" +
            "    }\n" +
            "}",
            "Symfony: Command must return an integer value"
        );
    }

    public void testCommandWithIntReturnTypeIsSkipped() {
        assertLocalInspectionNotContains("test.php", "<?php\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "\n" +
            "#[AsCommand(name: 'app:foobar')]\n" +
            "class FoobarCommand\n" +
            "{\n" +
            "    public function __invoke(): int\n" +
            "    {\n" +
            "        return '<caret>test';\n" +
            "    }\n" +
            "}",
            "Symfony: Command must return an integer value"
        );
    }

    public void testNonCommandClassIsIgnored() {
        assertLocalInspectionNotContains("test.php", "<?php\n" +
            "class FoobarClass\n" +
            "{\n" +
            "    public function __invoke(): string\n" +
            "    {\n" +
            "        return 'te<caret>st';\n" +
            "    }\n" +
            "}",
            "Symfony: Command must return an integer value"
        );
    }

    public void testCommandWithMultipleReturns() {
        assertLocalInspectionContains("test.php", "<?php\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "\n" +
            "#[AsCommand(name: 'app:foobar')]\n" +
            "class FoobarCommand\n" +
            "{\n" +
            "    public function __invoke()\n" +
            "    {\n" +
            "        if (true) {\n" +
            "            return 0;\n" +
            "        }\n" +
            "        return 'err<caret>or';\n" +
            "    }\n" +
            "}",
            "Symfony: Command must return an integer value"
        );
    }

    public void testTraditionalCommandWithoutAttributeIsIgnored() {
        assertLocalInspectionNotContains("test.php", "<?php\n" +
            "use Symfony\\Component\\Console\\Command\\Command;\n" +
            "\n" +
            "class FoobarCommand extends Command\n" +
            "{\n" +
            "    public function __invoke()\n" +
            "    {\n" +
            "        return 'inva<caret>lid';\n" +
            "    }\n" +
            "}",
            "Symfony: Command must return an integer value"
        );
    }
}
