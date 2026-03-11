package fr.adrienbrault.idea.symfony2plugin.tests.runAnything

import com.intellij.ide.actions.runAnything.items.RunAnythingItemBase
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import fr.adrienbrault.idea.symfony2plugin.runAnything.SymfonyConsoleRunAnythingProvider
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand

/**
 * @see SymfonyConsoleRunAnythingProvider
 */
class SymfonyConsoleRunAnythingProviderTest : SymfonyLightCodeInsightFixtureTestCase() {

    private val provider = SymfonyConsoleRunAnythingProvider()

    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("classes.php")
        myFixture.addFileToProject(
            "src/Command/CacheClearCommand.php",
            """
            <?php
            use Symfony\Component\Console\Attribute\AsCommand;
            use Symfony\Component\Console\Command\Command;

            #[AsCommand(name: 'cache:clear')]
            class CacheClearCommand extends Command {}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "src/Command/AppCreateUserCommand.php",
            """
            <?php
            use Symfony\Component\Console\Attribute\AsCommand;
            use Symfony\Component\Console\Command\Command;

            #[AsCommand(name: 'app:create-user')]
            class AppCreateUserCommand extends Command {}
            """.trimIndent()
        )
    }

    override fun getTestDataPath() =
        "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/command/fixtures"

    // --- getValues ---

    fun testGetValuesReturnsMatchingCommandsByPrefix() {
        val values = provider.getValues(createDataContext(), "cache")

        assertTrue(values.isNotEmpty())
        assertTrue(values.any { it.name == "cache:clear" })
    }

    fun testGetValuesEmptyPatternReturnsAllCommands() {
        val values = provider.getValues(createDataContext(), "")

        assertTrue(values.size >= 2)
    }

    fun testGetValuesNonMatchingPatternReturnsEmpty() {
        val values = provider.getValues(createDataContext(), "nonexistent:xyz:command")

        assertTrue(values.isEmpty())
    }

    fun testGetValuesCaseInsensitiveMatch() {
        val values = provider.getValues(createDataContext(), "CACHE")

        assertTrue(values.any { it.name == "cache:clear" })
    }

    // --- getMainListItem ---

    fun testGetMainListItemDescriptionIsShortClassName() {
        val cmd = SymfonyCommand("cache:clear", "\\App\\Command\\CacheClearCommand")
        val item = provider.getMainListItem(createDataContext(), cmd) as RunAnythingItemBase

        assertEquals("CacheClearCommand", item.description)
    }

    fun testGetMainListItemDescriptionForRootClass() {
        val cmd = SymfonyCommand("list", "\\ListCommand")
        val item = provider.getMainListItem(createDataContext(), cmd) as RunAnythingItemBase

        assertEquals("ListCommand", item.description)
    }

    fun testGetMainListItemCommand() {
        val cmd = SymfonyCommand("cache:clear", "\\App\\Command\\CacheClearCommand")
        val item = provider.getMainListItem(createDataContext(), cmd) as RunAnythingItemBase

        assertEquals("cache:clear", item.command)
    }

    // --- helpers ---

    private fun createDataContext() = SimpleDataContext.builder()
        .add(CommonDataKeys.PROJECT, project)
        .build()
}
