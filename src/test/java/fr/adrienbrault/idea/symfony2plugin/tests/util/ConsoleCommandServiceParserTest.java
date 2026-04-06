package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.dic.ConsoleCommandServiceParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.InputStream;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ConsoleCommandServiceParser
 */
public class ConsoleCommandServiceParserTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/fixtures";
    }

    /**
     * @see ConsoleCommandServiceParser#parser
     * @see ConsoleCommandServiceParser#getCommands
     */
    public void testParserExtractsCommandsFromTaggedServices() throws Exception {
        VirtualFile testFile = myFixture.copyFileToProject("SymfonyCommandUtilCompiledContainerTest.xml");

        ConsoleCommandServiceParser parser = new ConsoleCommandServiceParser();
        try (InputStream inputStream = testFile.getInputStream()) {
            parser.parser(inputStream, testFile, getProject());
        }

        Map<String, String> commands = parser.getCommands();

        assertTrue("doctrine:cache:clear-metadata should be present", commands.containsKey("doctrine:cache:clear-metadata"));
        assertEquals("\\Doctrine\\ORM\\Tools\\Console\\Command\\ClearCache\\MetadataCommand", commands.get("doctrine:cache:clear-metadata"));

        assertTrue("doctrine:schema:create should be present", commands.containsKey("doctrine:schema:create"));
        assertEquals("\\Doctrine\\ORM\\Tools\\Console\\Command\\SchemaTool\\CreateCommand", commands.get("doctrine:schema:create"));

        assertTrue("doctrine:schema:validate should be present", commands.containsKey("doctrine:schema:validate"));
        assertEquals("\\Doctrine\\ORM\\Tools\\Console\\Command\\ValidateSchemaCommand", commands.get("doctrine:schema:validate"));
    }

    /**
     * Services with a "console.command" tag but no "command" attribute should be ignored.
     */
    public void testParserIgnoresTagsWithoutCommandAttribute() throws Exception {
        VirtualFile testFile = myFixture.copyFileToProject("SymfonyCommandUtilCompiledContainerTest.xml");

        ConsoleCommandServiceParser parser = new ConsoleCommandServiceParser();
        try (InputStream inputStream = testFile.getInputStream()) {
            parser.parser(inputStream, testFile, getProject());
        }

        Map<String, String> commands = parser.getCommands();

        assertFalse("Services without command attribute should not appear", commands.containsValue("\\Symfony\\Bundle\\FrameworkBundle\\Command\\AboutCommand"));
    }

    /**
     * Services with unrelated tags should not appear as commands.
     */
    public void testParserIgnoresNonCommandTaggedServices() throws Exception {
        VirtualFile testFile = myFixture.copyFileToProject("SymfonyCommandUtilCompiledContainerTest.xml");

        ConsoleCommandServiceParser parser = new ConsoleCommandServiceParser();
        try (InputStream inputStream = testFile.getInputStream()) {
            parser.parser(inputStream, testFile, getProject());
        }

        Map<String, String> commands = parser.getCommands();

        assertFalse("Non-command services should not be included", commands.containsValue("\\App\\Service\\MyService"));
    }
}
