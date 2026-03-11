package fr.adrienbrault.idea.symfony2plugin.tests.codeInspection.command

import fr.adrienbrault.idea.symfony2plugin.codeInspection.command.CommandInvokableReturnValueInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see CommandInvokableReturnValueInspection
 */
class CommandInvokableReturnValueInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("classes.php")
    }

    override fun getTestDataPath(): String =
        "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/codeInspection/command/fixtures"

    fun testCommandReturningString() {
        assertLocalInspectionContains(
            "test.php", """
            <?php
            use Symfony\Component\Console\Attribute\AsCommand;
            
            #[AsCommand(name: 'app:foobar')]
            class FoobarCommand
            {
                public function __invoke()
                {
                    return '<caret>test';
                }
            }
            """.trimIndent(),
            CommandInvokableReturnValueInspection.MESSAGE
        )
    }

    fun testCommandReturningNull() {
        assertLocalInspectionContains(
            "test.php", """
            <?php
            use Symfony\Component\Console\Attribute\AsCommand;
            
            #[AsCommand(name: 'app:foobar')]
            class FoobarCommand
            {
                public function __invoke()
                {
                    return n<caret>ull;
                }
            }
            """.trimIndent(),
            CommandInvokableReturnValueInspection.MESSAGE
        )
    }

    fun testCommandReturningIntegerLiteral() {
        assertLocalInspectionNotContains(
            "test.php", """
            <?php
            use Symfony\Component\Console\Attribute\AsCommand;
            
            #[AsCommand(name: 'app:foobar')]
            class FoobarCommand
            {
                public function __invoke()
                {
                    return <caret>0;
                }
            }
            """.trimIndent(),
            CommandInvokableReturnValueInspection.MESSAGE
        )
    }

    fun testCommandReturningIntegerVariable() {
        assertLocalInspectionNotContains(
            "test.php", """
            <?php
            use Symfony\Component\Console\Attribute\AsCommand;
            
            #[AsCommand(name: 'app:foobar')]
            class FoobarCommand
            {
                public function __invoke()
                {
                    ${'$'}exitCode = 0;
                    return ${'$'}exit<caret>Code;
                }
            }
            """.trimIndent(),
            CommandInvokableReturnValueInspection.MESSAGE
        )
    }

    fun testCommandReturningCommandSuccess() {
        assertLocalInspectionNotContains(
            "test.php", """
            <?php
            use Symfony\Component\Console\Attribute\AsCommand;
            use Symfony\Component\Console\Command\Command;
            
            #[AsCommand(name: 'app:foobar')]
            class FoobarCommand
            {
                public function __invoke()
                {
                    return Command::SUC<caret>CESS;
                }
            }
            """.trimIndent(),
            CommandInvokableReturnValueInspection.MESSAGE
        )
    }

    fun testCommandWithIntReturnTypeIsSkipped() {
        assertLocalInspectionNotContains(
            "test.php", """
            <?php
            use Symfony\Component\Console\Attribute\AsCommand;
            
            #[AsCommand(name: 'app:foobar')]
            class FoobarCommand
            {
                public function __invoke(): int
                {
                    return '<caret>test';
                }
            }
            """.trimIndent(),
            CommandInvokableReturnValueInspection.MESSAGE
        )
    }

    fun testNonCommandClassIsIgnored() {
        assertLocalInspectionNotContains(
            "test.php", """
            <?php
            class FoobarClass
            {
                public function __invoke(): string
                {
                    return 'te<caret>st';
                }
            }
            """.trimIndent(),
            CommandInvokableReturnValueInspection.MESSAGE
        )
    }

    fun testCommandWithMultipleReturns() {
        assertLocalInspectionContains(
            "test.php", """
            <?php
            use Symfony\Component\Console\Attribute\AsCommand;
            
            #[AsCommand(name: 'app:foobar')]
            class FoobarCommand
            {
                public function __invoke()
                {
                    if (true) {
                        return 0;
                    }
                    return 'err<caret>or';
                }
            }
            """.trimIndent(),
            CommandInvokableReturnValueInspection.MESSAGE
        )
    }

    fun testTraditionalCommandWithoutAttributeIsIgnored() {
        assertLocalInspectionNotContains(
            "test.php", """
            <?php
            use Symfony\Component\Console\Command\Command;
            
            class FoobarCommand extends Command
            {
                public function __invoke()
                {
                    return 'inva<caret>lid';
                }
            }
            """.trimIndent(),
            CommandInvokableReturnValueInspection.MESSAGE
        )
    }
}
