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

    fun testInvalidReturnTypesAreReported() {
        assertContainsInspection(": void")
        assertContainsInspection(
            ": string",
            """
                return 'test';
            """.trimIndent()
        )
        assertContainsInspection("")
    }

    fun testSupportedAndIgnoredCasesDoNotReport() {
        assertNotContainsInspection(
            """
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
            """.trimIndent()
        )

        assertNotContainsInspection(
            """
            <?php
            class FoobarClass
            {
                public function __inv<caret>oke(): void
                {
                }
            }
            """.trimIndent()
        )

        assertNotContainsInspection(
            """
            <?php
            use Symfony\Component\Console\Command\Command;

            class FoobarCommand extends Command
            {
                public function __inv<caret>oke(): void
                {
                }
            }
            """.trimIndent()
        )
    }

    private fun assertContainsInspection(returnType: String, methodBody: String = "") {
        assertLocalInspectionContains("test.php", commandPhp(returnType, methodBody), CommandInvokableReturnTypeInspection.MESSAGE)
    }

    private fun assertNotContainsInspection(content: String) {
        assertLocalInspectionNotContains("test.php", content, CommandInvokableReturnTypeInspection.MESSAGE)
    }

    private fun commandPhp(returnType: String, methodBody: String = ""): String {
        val returnTypeSuffix = returnType.ifEmpty { "" }
        val body = methodBody.prependIndent("        ")

        return """
            <?php
            use Symfony\Component\Console\Attribute\AsCommand;

            #[AsCommand(name: 'app:foobar')]
            class FoobarCommand
            {
                public function __inv<caret>oke()$returnTypeSuffix
                {
        $body
                }
            }
            """.trimIndent()
    }
}
