package fr.adrienbrault.idea.symfony2plugin.tests.util;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;

import java.util.Collection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Tests command fallback from compiled container XML via {@link SymfonyCommandUtil}.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see SymfonyCommandUtil#getCommands
 */
public class SymfonyCommandUtilCompiledContainerTest extends SymfonyTempCodeInsightFixtureTestCase {

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
        createFile("var/cache/dev/App_KernelDevDebugContainer.xml", xml);

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
        createFile("var/cache/dev/App_KernelDevDebugContainer.xml", xml);

        Collection<SymfonyCommand> commands = SymfonyCommandUtil.getCommands(getProject());

        SymfonyCommand cmd = commands.stream()
            .filter(c -> "doctrine:cache:clear-metadata".equals(c.getName()))
            .findFirst().orElseThrow();

        assertEquals("\\Doctrine\\ORM\\Tools\\Console\\Command\\ClearCache\\MetadataCommand", cmd.getFqn());
    }
}
