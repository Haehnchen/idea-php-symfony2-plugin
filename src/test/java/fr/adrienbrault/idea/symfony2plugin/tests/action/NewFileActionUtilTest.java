package fr.adrienbrault.idea.symfony2plugin.tests.action;

import fr.adrienbrault.idea.symfony2plugin.action.NewFileActionUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NewFileActionUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/action/fixtures";
    }

    public void testGuessCommandTemplateTypeReturnsInvokableForNewNamespace() {
        String result = NewFileActionUtil.guessCommandTemplateType(getProject(), "App\\CommandNothing");
        assertEquals("command_invokable", result);
    }

    public void testGuessCommandTemplateTypeReturnsInvokableWhenExistingCommandUsesInvoke() {
        // Create a command with __invoke method (extends Command, not InvokableCommand)
        myFixture.addFileToProject(
            "src/Command/ExistingCommand.php",
            "<?php\n" +
                "namespace App\\Command;\n" +
                "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
                "use Symfony\\Component\\Console\\Style\\SymfonyStyle;\n" +
                "\n" +
                "#[AsCommand(name: 'app:existing')]\n" +
                "class ExistingCommand\n" +
                "{\n" +
                "    public function __invoke(SymfonyStyle $io): int\n" +
                "    {\n" +
                "        return Command::SUCCESS;\n" +
                "    }\n" +
                "}\n"
        );

        String result = NewFileActionUtil.guessCommandTemplateType(getProject(), "App\\Command");
        assertEquals("command_invokable", result);
    }

    public void testGuessCommandTemplateTypeFallsBackWhenExistingCommandUsesExecute() {
        // Create a command with execute method (uses execute, not __invoke)
        myFixture.configureByText(
            "ExistingCommand.php",
            "<?php\n" +
                "namespace App\\CommandConfigure;\n" +
                "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
                "use Symfony\\Component\\Console\\Command\\Command;\n" +
                "use Symfony\\Component\\Console\\Input\\InputInterface;\n" +
                "use Symfony\\Component\\Console\\Output\\OutputInterface;\n" +
                "\n" +
                "#[AsCommand(name: 'app:existing')]\n" +
                "class ExistingCommand extends Command\n" +
                "{\n" +
                "    protected function configure(): void\n" +
                "    {\n" +
                "        $this->setDescription('Test');\n" +
                "    }\n" +
                "\n" +
                "    protected function execute(InputInterface $input, OutputInterface $output): int\n" +
                "    {\n" +
                "        return Command::SUCCESS;\n" +
                "    }\n" +
                "}\n"
        );

        String result = NewFileActionUtil.guessCommandTemplateType(getProject(), "App\\CommandConfigure");
        assertEquals("command_attributes", result);
    }
}
