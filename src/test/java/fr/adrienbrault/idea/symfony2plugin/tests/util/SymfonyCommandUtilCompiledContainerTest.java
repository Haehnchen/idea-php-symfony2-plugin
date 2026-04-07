package fr.adrienbrault.idea.symfony2plugin.tests.util;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcherKt;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * Tests command fallback from compiled container XML via {@link SymfonyCommandUtil}.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see SymfonyCommandUtil#getCommands
 */
public class SymfonyCommandUtilCompiledContainerTest extends SymfonyLightCodeInsightFixtureTestCase {

    private static final String FIXTURE_PATH = "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/fixtures/SymfonyCommandUtilCompiledContainerTest.xml";

    /**
     * Commands found in the compiled container XML appear in {@code SymfonyCommandUtil.getCommands()}
     * as a fallback for commands without PHP source in the project.
     *
     * @see SymfonyCommandUtil#getCommands
     */
    public void testGetCommandsFallsBackToCompiledContainerForMissingCommands() {
        String xml;
        try {
            xml = new String(Files.readAllBytes(Paths.get(FIXTURE_PATH)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        createFileInProjectRoot("var/cache/dev/App_KernelDevDebugContainer.xml", xml);
        SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(getProject()).reloadConfiguration();

        Collection<SymfonyCommand> commands = SymfonyCommandUtil.getCommands(getProject());

        assertTrue(
            "doctrine:cache:clear-metadata from compiled container should appear as fallback",
            commands.stream().anyMatch(c -> "doctrine:cache:clear-metadata".equals(c.getName()))
        );

        assertTrue(
            "doctrine:schema:create from compiled container should appear as fallback",
            commands.stream().anyMatch(c -> "doctrine:schema:create".equals(c.getName()))
        );
    }

    /**
     * FQN from the compiled container XML is correctly mapped in the returned SymfonyCommand.
     *
     * @see SymfonyCommandUtil#getCommands
     */
    public void testGetCommandsFallsBackWithCorrectFqn() {
        String xml;
        try {
            xml = new String(Files.readAllBytes(Paths.get(FIXTURE_PATH)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        createFileInProjectRoot("var/cache/dev/App_KernelDevDebugContainer.xml", xml);
        SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(getProject()).reloadConfiguration();

        Collection<SymfonyCommand> commands = SymfonyCommandUtil.getCommands(getProject());

        SymfonyCommand cmd = commands.stream()
            .filter(c -> "doctrine:cache:clear-metadata".equals(c.getName()))
            .findFirst().orElseThrow();

        assertEquals("\\Doctrine\\ORM\\Tools\\Console\\Command\\ClearCache\\MetadataCommand", cmd.getFqn());
    }
}
