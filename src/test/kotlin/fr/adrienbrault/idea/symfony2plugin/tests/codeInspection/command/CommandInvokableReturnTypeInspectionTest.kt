package fr.adrienbrault.idea.symfony2plugin.tests.codeInspection.command

import fr.adrienbrault.idea.symfony2plugin.codeInspection.command.CommandInvokableReturnTypeInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see CommandInvokableReturnTypeInspection
 */
class CommandInvokableReturnTypeInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("classes.php")
    }

    override fun getTestDataPath(): String =
        "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/codeInspection/command/fixtures"

    fun testCommandWithoutIntReturnType() {
        assertLocalInspectionContains(
            "test.php", """
            <?php
            use Symfony\Component\Console\Attribute\AsCommand;
            
            #[AsCommand(name: 'app:foobar')]
            class FoobarCommand
            {
                public function __inv<caret>oke(): void
                {
                }
            }
            """.trimIndent(),
            CommandInvokableReturnTypeInspection.MESSAGE
        )
    }

    fun testCommandWithStringReturnType() {
        assertLocalInspectionContains(
            "test.php", """
            <?php
            use Symfony\Component\Console\Attribute\AsCommand;
            
            #[AsCommand(name: 'app:foobar')]
            class FoobarCommand
            {
                public function __inv<caret>oke(): string
                {
                    return 'test';
                }
            }
            """.trimIndent(),
            CommandInvokableReturnTypeInspection.MESSAGE
        )
    }

    fun testCommandWithNoReturnType() {
        assertLocalInspectionContains(
            "test.php", """
            <?php
            use Symfony\Component\Console\Attribute\AsCommand;
            
            #[AsCommand(name: 'app:foobar')]
            class FoobarCommand
            {
                public function __inv<caret>oke()
                {
                }
            }
            """.trimIndent(),
            CommandInvokableReturnTypeInspection.MESSAGE
        )
    }

    fun testCommandWithIntReturnType() {
        assertLocalInspectionNotContains(
            "test.php", """
            <?php
            use Symfony\Component\Console\Attribute\AsCommand;
            
            #[AsCommand(name: 'app:foobar')]
            class FoobarCommand
            {
                public function __inv<caret>oke(): int
                {
                    return 0;
                }
            }
            """.trimIndent(),
            CommandInvokableReturnTypeInspection.MESSAGE
        )
    }

    fun testNonCommandClassIsIgnored() {
        assertLocalInspectionNotContains(
            "test.php", """
            <?php
            class FoobarClass
            {
                public function __inv<caret>oke(): void
                {
                }
            }
            """.trimIndent(),
            CommandInvokableReturnTypeInspection.MESSAGE
        )
    }

    fun testTraditionalCommandWithoutAttributeIsIgnored() {
        assertLocalInspectionNotContains(
            "test.php", """
            <?php
            use Symfony\Component\Console\Command\Command;
            
            class FoobarCommand extends Command
            {
                public function __inv<caret>oke(): void
                {
                }
            }
            """.trimIndent(),
            CommandInvokableReturnTypeInspection.MESSAGE
        )
    }
}
