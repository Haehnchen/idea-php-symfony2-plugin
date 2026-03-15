package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.intellij.openapi.vfs.VfsUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.ConsoleCommandServiceParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

/**
 * Tests command extraction from compiled container XML via {@link ConsoleCommandServiceParser}.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ConsoleCommandServiceParser
 * @see SymfonyCommandUtil#getCommands
 */
public class SymfonyCommandUtilCompiledContainerTest extends SymfonyTempCodeInsightFixtureTestCase {

    private static final String FIXTURE_PATH = "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/fixtures/SymfonyCommandUtilCompiledContainerTest.xml";

    /**
     * @see ConsoleCommandServiceParser#parser
     * @see ConsoleCommandServiceParser#getCommands
     */
    public void testParserExtractsCommandsFromTaggedServices() throws Exception {
        File testFile = new File(FIXTURE_PATH);

        ConsoleCommandServiceParser parser = new ConsoleCommandServiceParser();
        parser.parser(new FileInputStream(testFile), VfsUtil.findFileByIoFile(testFile, true), getProject());

        Map<String, String> commands = parser.getCommands();

        assertTrue("doctrine:cache:clear-metadata should be present", commands.containsKey("doctrine:cache:clear-metadata"));
        assertEquals("\\Doctrine\\ORM\\Tools\\Console\\Command\\ClearCache\\MetadataCommand", commands.get("doctrine:cache:clear-metadata"));

        assertTrue("doctrine:schema:create should be present", commands.containsKey("doctrine:schema:create"));
        assertEquals("\\Doctrine\\ORM\\Tools\\Console\\Command\\SchemaTool\\CreateCommand", commands.get("doctrine:schema:create"));

        assertTrue("doctrine:schema:validate should be present", commands.containsKey("doctrine:schema:validate"));
        assertEquals("\\Doctrine\\ORM\\Tools\\Console\\Command\\ValidateSchemaCommand", commands.get("doctrine:schema:validate"));
    }

    /**
     * Services with a "console.command" tag but no "command" attribute (e.g., about command)
     * should be ignored — the command name cannot be determined from the tag attribute alone.
     *
     * @see ConsoleCommandServiceParser#parser
     */
    public void testParserIgnoresTagsWithoutCommandAttribute() throws Exception {
        File testFile = new File(FIXTURE_PATH);

        ConsoleCommandServiceParser parser = new ConsoleCommandServiceParser();
        parser.parser(new FileInputStream(testFile), VfsUtil.findFileByIoFile(testFile, true), getProject());

        Map<String, String> commands = parser.getCommands();

        // console.command.about has <tag name="console.command"/> without command= attribute — skip
        assertFalse("Services without command attribute should not appear", commands.containsValue("\\Symfony\\Bundle\\FrameworkBundle\\Command\\AboutCommand"));
    }

    /**
     * Services with unrelated tags should not appear as commands.
     *
     * @see ConsoleCommandServiceParser#parser
     */
    public void testParserIgnoresNonCommandTaggedServices() throws Exception {
        File testFile = new File(FIXTURE_PATH);

        ConsoleCommandServiceParser parser = new ConsoleCommandServiceParser();
        parser.parser(new FileInputStream(testFile), VfsUtil.findFileByIoFile(testFile, true), getProject());

        Map<String, String> commands = parser.getCommands();

        assertFalse("Non-command services should not be included", commands.containsValue("\\App\\Service\\MyService"));
    }

    /**
     * Commands found in the compiled container XML appear in {@code SymfonyCommandUtil.getCommands()}
     * as a fallback for commands without PHP source in the project.
     *
     * @see SymfonyCommandUtil#getCommands
     */
    public void testGetCommandsFallsBackToCompiledContainerForMissingCommands() throws IOException {
        String xml = new String(Files.readAllBytes(Paths.get(FIXTURE_PATH)));
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
    public void testGetCommandsFallsBackWithCorrectFqn() throws IOException {
        String xml = new String(Files.readAllBytes(Paths.get(FIXTURE_PATH)));
        createFile("var/cache/dev/App_KernelDevDebugContainer.xml", xml);

        Collection<SymfonyCommand> commands = SymfonyCommandUtil.getCommands(getProject());

        SymfonyCommand cmd = commands.stream()
            .filter(c -> "doctrine:cache:clear-metadata".equals(c.getName()))
            .findFirst().orElseThrow();

        assertEquals("\\Doctrine\\ORM\\Tools\\Console\\Command\\ClearCache\\MetadataCommand", cmd.getFqn());
    }
}
