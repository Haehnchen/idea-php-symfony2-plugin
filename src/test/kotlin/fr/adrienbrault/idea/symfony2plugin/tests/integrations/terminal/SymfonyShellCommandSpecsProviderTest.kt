package fr.adrienbrault.idea.symfony2plugin.tests.integrations.terminal

import com.intellij.testFramework.DumbModeTestUtils
import fr.adrienbrault.idea.symfony2plugin.integrations.terminal.SymfonyShellCommandSpecsProvider
import fr.adrienbrault.idea.symfony2plugin.integrations.terminal.collectCommandData
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import org.jetbrains.plugins.terminal.block.completion.spec.ShellCommandSpecConflictStrategy

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see SymfonyShellCommandSpecsProvider
 */
class SymfonyShellCommandSpecsProviderTest : SymfonyLightCodeInsightFixtureTestCase() {

    override fun getTestDataPath(): String =
        "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/integrations/terminal/fixtures"

    override fun setUp() {
        super.setUp()
        // Commands extending Command: found via subclass scan (multiple per file is fine)
        myFixture.copyFileToProject("TerminalCommandFixture.php")
        // Standalone invokable commands without extends Command: each needs its own file
        // (PhpAttributeIndex stores one entry per attribute key per file)
        myFixture.copyFileToProject("TerminalCommandOptionsFixture.php")
        myFixture.copyFileToProject("TerminalCommandArgsFixture.php")
    }

    // -------------------------------------------------------------------------
    // Static spec structure
    // -------------------------------------------------------------------------

    /**
     * @see SymfonyShellCommandSpecsProvider.getCommandSpecs
     */
    fun testGetCommandSpecsRegistersBinConsole() {
        val specs = SymfonyShellCommandSpecsProvider().getCommandSpecs()
        assertTrue(
            "bin/console spec must be registered",
            specs.any { it.spec.name == "bin/console" }
        )
    }

    /**
     * @see SymfonyShellCommandSpecsProvider.getCommandSpecs
     */
    fun testGetCommandSpecsRegistersSymfony() {
        val specs = SymfonyShellCommandSpecsProvider().getCommandSpecs()
        assertTrue(
            "symfony spec must be registered",
            specs.any { it.spec.name == "symfony" }
        )
    }

    /**
     * Both specs must use OVERRIDE so they take priority over any default shell completion.
     *
     * @see SymfonyShellCommandSpecsProvider.getCommandSpecs
     */
    fun testGetCommandSpecsUsesOverrideConflictStrategy() {
        val specs = SymfonyShellCommandSpecsProvider().getCommandSpecs()
        for (info in specs) {
            assertEquals(
                "Spec '${info.spec.name}' must use OVERRIDE conflict strategy",
                ShellCommandSpecConflictStrategy.OVERRIDE,
                info.conflictStrategy
            )
        }
    }

    // -------------------------------------------------------------------------
    // Command data collection
    // -------------------------------------------------------------------------

    /**
     * Commands discovered from PHP classes appear in the collected data.
     *
     * @see collectCommandData
     */
    fun testCollectCommandDataReturnsDiscoveredCommands() {
        val data = collectCommandData(project)
        val names = data.map { it.name }

        assertTrue("terminal:with-options should be collected", "terminal:with-options" in names)
        assertTrue("terminal:with-args should be collected", "terminal:with-args" in names)
        assertTrue("terminal:modern-options should be collected", "terminal:modern-options" in names)
    }

    fun testCollectCommandDataInvalidatesOnPhpModification() {
        val before = collectCommandData(project).map { it.name }
        assertFalse("terminal:after-cache should not exist before adding the PHP file", "terminal:after-cache" in before)

        myFixture.addFileToProject(
            "src/Command/AfterCacheCommand.php",
            """
            <?php

            namespace TerminalFixtures;

            use Symfony\Component\Console\Attribute\AsCommand;
            use Symfony\Component\Console\Command\Command;

            #[AsCommand(name: 'terminal:after-cache')]
            class AfterCacheCommand extends Command {}
            """.trimIndent()
        )

        val after = collectCommandData(project).map { it.name }
        assertTrue("terminal:after-cache should be collected after the PHP modification", "terminal:after-cache" in after)
    }

    fun testCollectCommandDataReturnsEmptyInDumbMode() {
        DumbModeTestUtils.runInDumbModeSynchronously(project) {
            val data = collectCommandData(project)

            assertTrue(data.isEmpty())
        }
    }

    /**
     * Options from `addOption()` calls are reflected with name and shortcut.
     *
     * @see collectCommandData
     */
    fun testCollectCommandDataIncludesOptionsFromAddOption() {
        val data = collectCommandData(project)
        val cmd = data.find { it.name == "terminal:with-options" }
        assertNotNull("terminal:with-options command must exist", cmd)

        assertTrue("option 'env' must be present", cmd!!.options.containsKey("env"))
        assertEquals("env shortcut must be 'e'", "e", cmd.options["env"]?.shortcut())
        assertTrue("option 'no-debug' must be present", cmd.options.containsKey("no-debug"))
    }

    /**
     * Options from `#[Option]` attributes on `__invoke()` are reflected.
     *
     * @see collectCommandData
     */
    fun testCollectCommandDataIncludesOptionsFromAttribute() {
        val data = collectCommandData(project)
        val cmd = data.find { it.name == "terminal:modern-options" }
        assertNotNull("terminal:modern-options command must exist", cmd)

        assertTrue("option 'idle' must be present", cmd!!.options.containsKey("idle"))
        assertTrue("option 'verbose' must be present", cmd.options.containsKey("verbose"))
        assertEquals("verbose shortcut must be 'v'", "v", cmd.options["verbose"]?.shortcut())
    }

    /**
     * A command using both `addOption()` and `#[Option]` attributes exposes both.
     *
     * @see collectCommandData
     */
    fun testCollectCommandDataIncludesOptionsMixedSources() {
        val data = collectCommandData(project)
        val cmd = data.find { it.name == "terminal:mixed" }
        assertNotNull("terminal:mixed command must exist", cmd)

        assertTrue("configure()-based option 'config' must be present", cmd!!.options.containsKey("config"))
        assertEquals("config shortcut must be 'c'", "c", cmd.options["config"]?.shortcut())
        assertTrue("#[Option]-based option 'force' must be present", cmd.options.containsKey("force"))
    }

    /**
     * Options from `setDefinition([new InputOption(...)])` are reflected.
     *
     * @see collectCommandData
     */
    fun testCollectCommandDataIncludesOptionsFromSetDefinition() {
        val data = collectCommandData(project)
        val cmd = data.find { it.name == "terminal:with-definition" }
        assertNotNull("terminal:with-definition command must exist", cmd)

        assertTrue("option 'format' must be present", cmd!!.options.containsKey("format"))
        assertEquals("format shortcut must be 'f'", "f", cmd.options["format"]?.shortcut())
        assertTrue("option 'verbose' must be present", cmd.options.containsKey("verbose"))
    }

    /**
     * Arguments from `addArgument()` calls appear in the collected data.
     *
     * @see collectCommandData
     */
    fun testCollectCommandDataIncludesArgumentsFromAddArgument() {
        val data = collectCommandData(project)
        val cmd = data.find { it.name == "terminal:with-args" }
        assertNotNull("terminal:with-args command must exist", cmd)

        assertTrue("argument 'username' must be present", cmd!!.arguments.containsKey("username"))
        assertTrue("argument 'role' must be present", cmd.arguments.containsKey("role"))
    }

    /**
     * Arguments from `#[Argument]` attributes on `__invoke()` are reflected.
     *
     * @see collectCommandData
     */
    fun testCollectCommandDataIncludesArgumentsFromAttribute() {
        val data = collectCommandData(project)
        val cmd = data.find { it.name == "terminal:modern-args" }
        assertNotNull("terminal:modern-args command must exist", cmd)

        assertTrue("argument 'username' must be present", cmd!!.arguments.containsKey("username"))
        assertTrue("argument 'user-id' must be present", cmd.arguments.containsKey("user-id"))
    }

    /**
     * A command using both `addArgument()` and `#[Argument]` attributes exposes both.
     *
     * @see collectCommandData
     */
    fun testCollectCommandDataIncludesArgumentsMixedSources() {
        val data = collectCommandData(project)
        val cmd = data.find { it.name == "terminal:mixed-args" }
        assertNotNull("terminal:mixed-args command must exist", cmd)

        assertTrue("addArgument()-based 'target' must be present", cmd!!.arguments.containsKey("target"))
        assertTrue("#[Argument]-based 'source' must be present", cmd.arguments.containsKey("source"))
    }

    /**
     * A command that only defines options should have an empty arguments map.
     *
     * @see collectCommandData
     */
    fun testCollectCommandDataOptionsOnlyCommandHasNoArguments() {
        val data = collectCommandData(project)
        val cmd = data.find { it.name == "terminal:with-options" }
        assertNotNull("terminal:with-options must exist", cmd)
        assertTrue("terminal:with-options should have no arguments", cmd!!.arguments.isEmpty())
    }
}
