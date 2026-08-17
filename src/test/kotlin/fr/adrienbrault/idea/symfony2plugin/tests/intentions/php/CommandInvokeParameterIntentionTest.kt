package fr.adrienbrault.idea.symfony2plugin.tests.intentions.php

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.intentions.php.CommandInvokeParameterIntention
import fr.adrienbrault.idea.symfony2plugin.intentions.php.getAvailableCommandParameterFqns
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see CommandInvokeParameterIntention
 */
class CommandInvokeParameterIntentionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"))
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/intentions/php/fixtures"
    }

    fun testIntentionIsAvailableForInvokableCommand() {
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Command;\n" +
                "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
                "\n" +
                "#[AsCommand(name: 'app:test')]\n" +
                "class <caret>TestCommand\n" +
                "{\n" +
                "    public function __invoke(): int\n" +
                "    {\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n",
            "Symfony: Add parameter to __invoke"
        )
    }

    fun testIntentionIsNotAvailableWithoutInvokeMethod() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Command;\n" +
                "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
                "\n" +
                "#[AsCommand(name: 'app:test')]\n" +
                "class <caret>TestCommand\n" +
                "{\n" +
                "    public function execute(): int\n" +
                "    {\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n"
        )

        assertFalse(
            myFixture.filterAvailableIntentions("Symfony: Add parameter to __invoke")
                .any { action -> action.text == "Symfony: Add parameter to __invoke" }
        )
    }

    fun testIntentionIsNotAvailableForClassExtendingCommand() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Command;\n" +
                "use Symfony\\Component\\Console\\Command\\Command;\n" +
                "\n" +
                "class <caret>TestCommand extends Command\n" +
                "{\n" +
                "    public function __invoke(): int\n" +
                "    {\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n"
        )

        assertFalse(
            myFixture.filterAvailableIntentions("Symfony: Add parameter to __invoke")
                .any { action -> action.text == "Symfony: Add parameter to __invoke" }
        )
    }

    fun testIntentionIsNotAvailableWithoutAsCommandAttribute() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Command;\n" +
                "\n" +
                "class <caret>TestCommand\n" +
                "{\n" +
                "    public function __invoke(): int\n" +
                "    {\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n"
        )

        assertFalse(
            myFixture.filterAvailableIntentions("Symfony: Add parameter to __invoke")
                .any { action -> action.text == "Symfony: Add parameter to __invoke" }
        )
    }

    fun testIntentionIsNotAvailableWhenAllParametersExist() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Command;\n" +
                "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
                "use Symfony\\Component\\Console\\Input\\InputInterface;\n" +
                "use Symfony\\Component\\Console\\Output\\OutputInterface;\n" +
                "use Symfony\\Component\\Console\\Cursor;\n" +
                "use Symfony\\Component\\Console\\Style\\SymfonyStyle;\n" +
                "use Symfony\\Component\\Console\\Application;\n" +
                "\n" +
                "#[AsCommand(name: 'app:test')]\n" +
                "class <caret>TestCommand\n" +
                "{\n" +
                "    public function __invoke(\n" +
                "        InputInterface \$input,\n" +
                "        OutputInterface \$output,\n" +
                "        Cursor \$cursor,\n" +
                "        SymfonyStyle \$io,\n" +
                "        Application \$application\n" +
                "    ): int {\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n"
        )

        assertFalse(
            myFixture.filterAvailableIntentions("Symfony: Add parameter to __invoke")
                .any { action -> action.text == "Symfony: Add parameter to __invoke" }
        )
    }

    fun testGetAvailableParameterFqnsReturnsAllWhenEmpty() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Command;\n" +
                "\n" +
                "class TestCommand\n" +
                "{\n" +
                "    public function __invoke(): int\n" +
                "    {\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n"
        )

        val phpClass = PsiTreeUtil.findChildOfType(myFixture.file, PhpClass::class.java)
        assertNotNull(phpClass)

        val invokeMethod = phpClass!!.findOwnMethodByName("__invoke")
        assertNotNull(invokeMethod)

        val availableParams = getAvailableCommandParameterFqns(invokeMethod!!)

        assertEquals(5, availableParams.size)
        assertTrue(availableParams.contains("Symfony\\Component\\Console\\Input\\InputInterface"))
        assertTrue(availableParams.contains("Symfony\\Component\\Console\\Output\\OutputInterface"))
        assertTrue(availableParams.contains("Symfony\\Component\\Console\\Cursor"))
        assertTrue(availableParams.contains("Symfony\\Component\\Console\\Style\\SymfonyStyle"))
        assertTrue(availableParams.contains("Symfony\\Component\\Console\\Application"))
    }

    fun testGetAvailableParameterFqnsFiltersExisting() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Command;\n" +
                "use Symfony\\Component\\Console\\Style\\SymfonyStyle;\n" +
                "\n" +
                "class TestCommand\n" +
                "{\n" +
                "    public function __invoke(SymfonyStyle \$io): int\n" +
                "    {\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n"
        )

        val phpClass = PsiTreeUtil.findChildOfType(myFixture.file, PhpClass::class.java)
        assertNotNull(phpClass)

        val invokeMethod = phpClass!!.findOwnMethodByName("__invoke")
        assertNotNull(invokeMethod)

        val availableParams = getAvailableCommandParameterFqns(invokeMethod!!)

        assertEquals(4, availableParams.size)
        assertFalse(availableParams.contains("Symfony\\Component\\Console\\Style\\SymfonyStyle"))
        assertTrue(availableParams.contains("Symfony\\Component\\Console\\Input\\InputInterface"))
        assertTrue(availableParams.contains("Symfony\\Component\\Console\\Output\\OutputInterface"))
        assertTrue(availableParams.contains("Symfony\\Component\\Console\\Cursor"))
        assertTrue(availableParams.contains("Symfony\\Component\\Console\\Application"))
    }
}
