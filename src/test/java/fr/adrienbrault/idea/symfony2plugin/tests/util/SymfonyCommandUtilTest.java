package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;

import java.util.Collection;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("SymfonyCommandUtilTest.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/fixtures";
    }

    /**
     * @see SymfonyCommandUtil#getCommands
     */
    public void testGetCommands() {
        Collection<SymfonyCommand> commands = SymfonyCommandUtil.getCommands(getProject());

        for (String s : new String[]{"foo", "property", "const", "app:create-user-1", "app:create-user-2", "app:create-user-3"}) {
            SymfonyCommand command = commands.stream()
                .filter(symfonyCommand -> s.equals(symfonyCommand.getName())).findFirst()
                .orElseThrow();

            assertNotNull(command);
        }
    }

    /**
     * @see SymfonyCommandUtil#getCommandOptions
     */
    public void testGetCommandOptionsFromTraditionalCommand() {
        Collection<PhpClass> phpClasses = PhpIndex.getInstance(getProject()).getAnyByFQN("\\CommandOptions\\TraditionalCommand");
        assertFalse("TraditionalCommand class should exist", phpClasses.isEmpty());
        PhpClass phpClass = phpClasses.iterator().next();

        Map<String, SymfonyCommandUtil.CommandOption> options = SymfonyCommandUtil.getCommandOptions(phpClass);

        assertEquals("Should have 2 options", 2, options.size());

        // Check 'name' option
        assertTrue("Should contain option 'name'", options.containsKey("name"));
        SymfonyCommandUtil.CommandOption nameOption = options.get("name");
        assertEquals("name shortcut should be null", null, nameOption.shortcut());

        // Check 'last_name' option with shortcut 'd'
        assertTrue("Should contain option 'last_name'", options.containsKey("last_name"));
        SymfonyCommandUtil.CommandOption lastNameOption = options.get("last_name");
        assertEquals("last_name shortcut should be 'd'", "d", lastNameOption.shortcut());
    }

    /**
     * @see SymfonyCommandUtil#getCommandOptions
     */
    public void testGetCommandOptionsFromModernCommand() {
        Collection<PhpClass> phpClasses = PhpIndex.getInstance(getProject()).getAnyByFQN("\\CommandOptions\\ModernCommand");
        assertFalse("ModernCommand class should exist", phpClasses.isEmpty());
        PhpClass phpClass = phpClasses.iterator().next();

        Map<String, SymfonyCommandUtil.CommandOption> options = SymfonyCommandUtil.getCommandOptions(phpClass);

        assertEquals("Should have 4 options", 4, options.size());

        // Check 'idle' option (name explicitly set)
        assertTrue("Should contain option 'idle'", options.containsKey("idle"));
        assertEquals("idle shortcut should be null", null, options.get("idle").shortcut());

        // Check 'type' option (name from parameter)
        assertTrue("Should contain option 'type'", options.containsKey("type"));
        assertEquals("type shortcut should be null", null, options.get("type").shortcut());

        // Check 'verbose' option with shortcut 'v'
        assertTrue("Should contain option 'verbose'", options.containsKey("verbose"));
        assertEquals("verbose shortcut should be 'v'", "v", options.get("verbose").shortcut());

        // Check 'groups' option
        assertTrue("Should contain option 'groups'", options.containsKey("groups"));
        assertEquals("groups shortcut should be null", null, options.get("groups").shortcut());
    }

    /**
     * @see SymfonyCommandUtil#getCommandOptions
     */
    public void testGetCommandOptionsFromMixedCommand() {
        Collection<PhpClass> phpClasses = PhpIndex.getInstance(getProject()).getAnyByFQN("\\CommandOptions\\MixedCommand");
        assertFalse("MixedCommand class should exist", phpClasses.isEmpty());
        PhpClass phpClass = phpClasses.iterator().next();

        Map<String, SymfonyCommandUtil.CommandOption> options = SymfonyCommandUtil.getCommandOptions(phpClass);

        assertEquals("Should have 2 options (1 from configure, 1 from __invoke)", 2, options.size());

        // Check 'config' option from configure() with shortcut 'c'
        assertTrue("Should contain option 'config'", options.containsKey("config"));
        assertEquals("config shortcut should be 'c'", "c", options.get("config").shortcut());

        // Check 'force' option from #[Option]
        assertTrue("Should contain option 'force'", options.containsKey("force"));
        assertEquals("force shortcut should be null", null, options.get("force").shortcut());
    }

    /**
     * @see SymfonyCommandUtil#getCommandOptions
     */
    public void testGetCommandOptionsFromSetDefinitionArray() {
        Collection<PhpClass> phpClasses = PhpIndex.getInstance(getProject()).getAnyByFQN("\\CommandOptions\\DefinitionCommand");
        assertFalse("DefinitionCommand class should exist", phpClasses.isEmpty());
        PhpClass phpClass = phpClasses.iterator().next();

        Map<String, SymfonyCommandUtil.CommandOption> options = SymfonyCommandUtil.getCommandOptions(phpClass);

        assertEquals("Should have 2 options", 2, options.size());

        // Check 'format' option with shortcut 'f'
        assertTrue("Should contain option 'format'", options.containsKey("format"));
        assertEquals("format shortcut should be 'f'", "f", options.get("format").shortcut());

        // Check 'verbose' option with shortcut 'v'
        assertTrue("Should contain option 'verbose'", options.containsKey("verbose"));
        assertEquals("verbose shortcut should be 'v'", "v", options.get("verbose").shortcut());
    }

    /**
     * @see SymfonyCommandUtil#getCommandOptions
     */
    public void testGetCommandOptionsFromSetDefinitionSingle() {
        Collection<PhpClass> phpClasses = PhpIndex.getInstance(getProject()).getAnyByFQN("\\CommandOptions\\DefinitionSingleCommand");
        assertFalse("DefinitionSingleCommand class should exist", phpClasses.isEmpty());
        PhpClass phpClass = phpClasses.iterator().next();

        Map<String, SymfonyCommandUtil.CommandOption> options = SymfonyCommandUtil.getCommandOptions(phpClass);

        assertEquals("Should have 1 option", 1, options.size());

        // Check 'debug' option with shortcut 'd'
        assertTrue("Should contain option 'debug'", options.containsKey("debug"));
        assertEquals("debug shortcut should be 'd'", "d", options.get("debug").shortcut());
    }

    /**
     * @see SymfonyCommandUtil#getCommandOptions
     */
    public void testGetCommandOptionsFromEmptyCommand() {
        Collection<PhpClass> phpClasses = PhpIndex.getInstance(getProject()).getAnyByFQN("\\Foo\\FooCommand");
        assertFalse("FooCommand class should exist", phpClasses.isEmpty());
        PhpClass phpClass = phpClasses.iterator().next();

        Map<String, SymfonyCommandUtil.CommandOption> options = SymfonyCommandUtil.getCommandOptions(phpClass);

        assertEquals("Should have 0 options", 0, options.size());
    }

    /**
     * @see SymfonyCommandUtil#getCommandArguments
     */
    public void testGetCommandArgumentsFromTraditionalCommand() {
        Collection<PhpClass> phpClasses = PhpIndex.getInstance(getProject()).getAnyByFQN("\\CommandArguments\\TraditionalArgsCommand");
        assertFalse("TraditionalArgsCommand class should exist", phpClasses.isEmpty());
        PhpClass phpClass = phpClasses.iterator().next();

        java.util.Map<String, SymfonyCommandUtil.CommandArgument> arguments = SymfonyCommandUtil.getCommandArguments(phpClass);

        assertEquals("Should have 2 arguments", 2, arguments.size());

        // Check 'username' argument
        assertTrue("Should contain argument 'username'", arguments.containsKey("username"));
        SymfonyCommandUtil.CommandArgument username = arguments.get("username");
        assertNotNull("username argument should not be null", username);
        assertEquals("username name should match", "username", username.name());
        assertEquals("username description should match", "The username", username.description());
        assertNull("username should have no default value", username.defaultValue());
        assertNotNull("username should have target", username.target());

        // Check 'password' argument with default value
        assertTrue("Should contain argument 'password'", arguments.containsKey("password"));
        SymfonyCommandUtil.CommandArgument password = arguments.get("password");
        assertNotNull("password argument should not be null", password);
        assertEquals("password name should match", "password", password.name());
        assertEquals("password description should match", "The password", password.description());
        assertEquals("password default value should match", "'default123'", password.defaultValue());
        assertNotNull("password should have target", password.target());
    }

    /**
     * @see SymfonyCommandUtil#getCommandArguments
     */
    public void testGetCommandArgumentsFromModernCommand() {
        Collection<PhpClass> phpClasses = PhpIndex.getInstance(getProject()).getAnyByFQN("\\CommandArguments\\ModernArgsCommand");
        assertFalse("ModernArgsCommand class should exist", phpClasses.isEmpty());
        PhpClass phpClass = phpClasses.iterator().next();

        java.util.Map<String, SymfonyCommandUtil.CommandArgument> arguments = SymfonyCommandUtil.getCommandArguments(phpClass);

        assertEquals("Should have 3 arguments", 3, arguments.size());

        // Check 'name' argument (using parameter name)
        assertTrue("Should contain argument 'name'", arguments.containsKey("name"));
        SymfonyCommandUtil.CommandArgument name = arguments.get("name");
        assertEquals("name description should match", "The user name", name.description());

        // Check 'user-id' argument (explicit name)
        assertTrue("Should contain argument 'user-id'", arguments.containsKey("user-id"));
        SymfonyCommandUtil.CommandArgument userId = arguments.get("user-id");
        assertEquals("user-id description should match", "The user ID", userId.description());

        // Check 'email' argument with default value
        assertTrue("Should contain argument 'email'", arguments.containsKey("email"));
        SymfonyCommandUtil.CommandArgument email = arguments.get("email");
        assertEquals("email default value should match", "null", email.defaultValue());
    }

    /**
     * @see SymfonyCommandUtil#getCommandArguments
     */
    public void testGetCommandArgumentsFromSetDefinition() {
        Collection<PhpClass> phpClasses = PhpIndex.getInstance(getProject()).getAnyByFQN("\\CommandArguments\\DefinitionArgsCommand");
        assertFalse("DefinitionArgsCommand class should exist", phpClasses.isEmpty());
        PhpClass phpClass = phpClasses.iterator().next();

        java.util.Map<String, SymfonyCommandUtil.CommandArgument> arguments = SymfonyCommandUtil.getCommandArguments(phpClass);

        assertEquals("Should have 2 arguments", 2, arguments.size());

        // Check 'source' argument
        assertTrue("Should contain argument 'source'", arguments.containsKey("source"));
        SymfonyCommandUtil.CommandArgument source = arguments.get("source");
        assertEquals("source name should match", "source", source.name());
        assertEquals("source description should match", "Source file", source.description());
        assertNull("source should have no default value", source.defaultValue());

        // Check 'destination' argument with default value
        assertTrue("Should contain argument 'destination'", arguments.containsKey("destination"));
        SymfonyCommandUtil.CommandArgument destination = arguments.get("destination");
        assertEquals("destination name should match", "destination", destination.name());
        assertEquals("destination description should match", "Destination file", destination.description());
        assertEquals("destination default value should match", "'/tmp/default'", destination.defaultValue());
    }

    /**
     * @see SymfonyCommandUtil#getCommandArguments
     */
    public void testGetCommandArgumentsFromMixedCommand() {
        Collection<PhpClass> phpClasses = PhpIndex.getInstance(getProject()).getAnyByFQN("\\CommandArguments\\MixedArgsCommand");
        assertFalse("MixedArgsCommand class should exist", phpClasses.isEmpty());
        PhpClass phpClass = phpClasses.iterator().next();

        java.util.Map<String, SymfonyCommandUtil.CommandArgument> arguments = SymfonyCommandUtil.getCommandArguments(phpClass);

        assertEquals("Should have 2 arguments (1 from configure, 1 from __invoke)", 2, arguments.size());

        // Check 'config-file' argument from configure()
        assertTrue("Should contain argument 'config-file'", arguments.containsKey("config-file"));
        SymfonyCommandUtil.CommandArgument configFile = arguments.get("config-file");
        assertEquals("config-file description should match", "Config file path", configFile.description());

        // Check 'force' argument from #[Argument]
        assertTrue("Should contain argument 'force'", arguments.containsKey("force"));
        SymfonyCommandUtil.CommandArgument force = arguments.get("force");
        assertEquals("force default value should match", "false", force.defaultValue());
    }

    /**
     * @see SymfonyCommandUtil#getCommandArguments
     */
    public void testGetCommandArgumentsFromEmptyCommand() {
        Collection<PhpClass> phpClasses = PhpIndex.getInstance(getProject()).getAnyByFQN("\\Foo\\FooCommand");
        assertFalse("FooCommand class should exist", phpClasses.isEmpty());
        PhpClass phpClass = phpClasses.iterator().next();

        java.util.Map<String, SymfonyCommandUtil.CommandArgument> arguments = SymfonyCommandUtil.getCommandArguments(phpClass);

        assertEquals("Should have 0 arguments", 0, arguments.size());
    }
}
