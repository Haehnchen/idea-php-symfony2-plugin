package fr.adrienbrault.idea.symfony2plugin.tests.completion.command;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;

/**
 * Tests for {@link fr.adrienbrault.idea.symfony2plugin.completion.command.CommandNameTerminalCompletionContributor}
 * and {@link SymfonyCommandUtil#isSymfonyConsoleCommand}.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CommandNameTerminalCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    /**
     * @see SymfonyCommandUtil#isSymfonyConsoleCommand
     */
    public void testIsSymfonyConsoleCommandWithBinConsolePrefix() {
        assertTrue("bin/console should be valid", SymfonyCommandUtil.isSymfonyConsoleCommand("bin/console"));
        assertTrue("bin/console cache:clear should be valid", SymfonyCommandUtil.isSymfonyConsoleCommand("bin/console cache:clear"));
        assertTrue("bin/console debug:con should be valid", SymfonyCommandUtil.isSymfonyConsoleCommand("bin/console debug:con"));
        assertTrue("  bin/console  with spaces should be valid", SymfonyCommandUtil.isSymfonyConsoleCommand("  bin/console  "));

        assertTrue("symfony console should be valid", SymfonyCommandUtil.isSymfonyConsoleCommand("symfony console"));
        assertTrue("symfony console cache:clear should be valid", SymfonyCommandUtil.isSymfonyConsoleCommand("symfony console cache:clear"));
        assertTrue("symfony console debug:con should be valid", SymfonyCommandUtil.isSymfonyConsoleCommand("symfony console debug:con"));
        assertTrue("  symfony console  with spaces should be valid", SymfonyCommandUtil.isSymfonyConsoleCommand("  symfony console  "));
    }

    /**
     * @see SymfonyCommandUtil#isSymfonyConsoleCommand
     */
    public void testIsSymfonyConsoleCommandWithInvalidPrefixes() {
        assertFalse("php bin/console should not be valid", SymfonyCommandUtil.isSymfonyConsoleCommand("php bin/console"));
        assertFalse("bin/other should not be valid", SymfonyCommandUtil.isSymfonyConsoleCommand("bin/other"));
        assertFalse("console:cache should not be valid", SymfonyCommandUtil.isSymfonyConsoleCommand("console:cache"));
        assertFalse("random command should not be valid", SymfonyCommandUtil.isSymfonyConsoleCommand("random command"));
        assertFalse("symfony should not be valid without console", SymfonyCommandUtil.isSymfonyConsoleCommand("symfony"));
        assertFalse("symfony cache:clear should not be valid", SymfonyCommandUtil.isSymfonyConsoleCommand("symfony cache:clear"));
        assertFalse("sf console should not be valid", SymfonyCommandUtil.isSymfonyConsoleCommand("sf console"));
    }

    /**
     * @see SymfonyCommandUtil#isSymfonyConsoleCommand
     */
    public void testIsSymfonyConsoleCommandWithEmptyAndBlank() {
        assertFalse("empty string should not be valid", SymfonyCommandUtil.isSymfonyConsoleCommand(""));
        assertFalse("blank string should not be valid", SymfonyCommandUtil.isSymfonyConsoleCommand("   "));
    }
}
