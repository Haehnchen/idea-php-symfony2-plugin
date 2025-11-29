package fr.adrienbrault.idea.symfony2plugin.tests.intentions.php;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.php.CommandToInvokableIntention
 */
public class CommandToInvokableIntentionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/intentions/php/fixtures";
    }

    public void testIntentionIsAvailableForCommandWithExecute() {
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Command;\n" +
                "use Symfony\\Component\\Console\\Command\\Command;\n" +
                "use Symfony\\Component\\Console\\Input\\InputInterface;\n" +
                "use Symfony\\Component\\Console\\Output\\OutputInterface;\n" +
                "\n" +
                "class <caret>TestCommand extends Command\n" +
                "{\n" +
                "    protected function execute(InputInterface $input, OutputInterface $output): int\n" +
                "    {\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n",
            "Symfony: Migrate to invokable command"
        );
    }

    public void testIntentionIsNotAvailableWhenInvokeExists() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Command;\n" +
                "use Symfony\\Component\\Console\\Command\\Command;\n" +
                "use Symfony\\Component\\Console\\Style\\SymfonyStyle;\n" +
                "\n" +
                "class <caret>TestCommand extends Command\n" +
                "{\n" +
                "    public function __invoke(SymfonyStyle $io): int\n" +
                "    {\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n"
        );

        // Intention should not be available if __invoke already exists
        assertFalse(
            myFixture.filterAvailableIntentions("Symfony: Migrate to invokable command")
                .stream()
                .anyMatch(action -> action.getText().equals("Symfony: Migrate to invokable command"))
        );
    }

    public void testIntentionIsNotAvailableForNonCommandClass() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Service;\n" +
                "\n" +
                "class <caret>TestService\n" +
                "{\n" +
                "    public function execute(): int\n" +
                "    {\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n"
        );

        // Intention should not be available for non-Command classes
        assertFalse(
            myFixture.filterAvailableIntentions("Symfony: Migrate to invokable command")
                .stream()
                .anyMatch(action -> action.getText().equals("Symfony: Migrate to invokable command"))
        );
    }

    public void testMigrationWithArgumentsAndOptions() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Command;\n" +
                "use Symfony\\Component\\Console\\Command\\Command;\n" +
                "use Symfony\\Component\\Console\\Input\\InputArgument;\n" +
                "use Symfony\\Component\\Console\\Input\\InputInterface;\n" +
                "use Symfony\\Component\\Console\\Input\\InputOption;\n" +
                "use Symfony\\Component\\Console\\Output\\OutputInterface;\n" +
                "\n" +
                "class <caret>TestCommand extends Command\n" +
                "{\n" +
                "    protected function configure(): void\n" +
                "    {\n" +
                "        $this->addArgument('name', InputArgument::REQUIRED, 'The name');\n" +
                "        $this->addOption('admin', null, InputOption::VALUE_NONE, 'Make admin');\n" +
                "    }\n" +
                "\n" +
                "    protected function execute(InputInterface $input, OutputInterface $output): int\n" +
                "    {\n" +
                "        $name = $input->getArgument('name');\n" +
                "        $admin = $input->getOption('admin');\n" +
                "        $output->writeln('Hello ' . $name);\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n"
        );

        // Apply the intention
        var intention = myFixture.findSingleIntention("Symfony: Migrate to invokable command");
        myFixture.launchAction(intention);

        // Verify the result
        String result = myFixture.getFile().getText();

        // Check that __invoke method exists
        assertTrue("Should have __invoke method", result.contains("public function __invoke("));

        // Check that execute method is gone
        assertFalse("Should not have execute method", result.contains("protected function execute("));

        // Check that configure method is removed
        assertFalse("Should not have configure method", result.contains("protected function configure("));

        // Check that arguments are replaced with direct variable access
        assertTrue("Should use $name directly", result.contains("'Hello ' . $name"));

        // Check that Argument and Option attributes are present
        assertTrue("Should have Argument attribute", result.contains("Argument("));
        assertTrue("Should have Option attribute", result.contains("Option("));
    }

    public void testMigrationKeepsOutputWhenUsed() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Command;\n" +
                "use Symfony\\Component\\Console\\Command\\Command;\n" +
                "use Symfony\\Component\\Console\\Input\\InputArgument;\n" +
                "use Symfony\\Component\\Console\\Input\\InputInterface;\n" +
                "use Symfony\\Component\\Console\\Output\\OutputInterface;\n" +
                "\n" +
                "class <caret>TestCommand extends Command\n" +
                "{\n" +
                "    protected function configure(): void\n" +
                "    {\n" +
                "        $this->addArgument('name', InputArgument::REQUIRED, 'The name');\n" +
                "    }\n" +
                "\n" +
                "    protected function execute(InputInterface $input, OutputInterface $output): int\n" +
                "    {\n" +
                "        $name = $input->getArgument('name');\n" +
                "        $output->writeln('Hello ' . $name);\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n"
        );

        // Apply the intention
        var intention = myFixture.findSingleIntention("Symfony: Migrate to invokable command");
        myFixture.launchAction(intention);

        String result = myFixture.getFile().getText();

        // Should keep OutputInterface since it's used
        assertTrue("Should keep OutputInterface parameter", result.matches("(?s).*OutputInterface\\s+\\$output.*"));

        // Should not keep InputInterface since only used for getArgument which is migrated
        assertFalse("Should not keep InputInterface parameter", result.matches("(?s).*InputInterface\\s+\\$output.*"));

        // Should have the argument parameter
        assertTrue("Should have name parameter", result.contains("string $name"));
    }

    public void testMigrationDropsUnusedParameters() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Command;\n" +
                "use Symfony\\Component\\Console\\Command\\Command;\n" +
                "use Symfony\\Component\\Console\\Input\\InputArgument;\n" +
                "use Symfony\\Component\\Console\\Input\\InputInterface;\n" +
                "use Symfony\\Component\\Console\\Input\\InputOption;\n" +
                "use Symfony\\Component\\Console\\Output\\OutputInterface;\n" +
                "\n" +
                "class <caret>TestCommand extends Command\n" +
                "{\n" +
                "    protected function configure(): void\n" +
                "    {\n" +
                "        $this->addArgument('name', InputArgument::REQUIRED, 'The name');\n" +
                "        $this->addOption('yell', null, InputOption::VALUE_NONE, 'Yell?');\n" +
                "    }\n" +
                "\n" +
                "    protected function execute(InputInterface $input, OutputInterface $output): int\n" +
                "    {\n" +
                "        // Using $input for getArgument/getOption - should keep it\n" +
                "        $name = $input->getArgument('name');\n" +
                "        $yell = $input->getOption('yell');\n" +
                "        // Not using $output at all\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n"
        );

        // Apply the intention
        var intention = myFixture.findSingleIntention("Symfony: Migrate to invokable command");
        myFixture.launchAction(intention);

        String result = myFixture.getFile().getText();

        // Should keep InputInterface since it's used for getArgument/getOption
        assertTrue("Should keep InputInterface parameter", result.contains("InputInterface") && result.contains("$input"));

        // Should not keep OutputInterface since it's not used
        assertFalse("Should not keep OutputInterface parameter", result.contains("OutputInterface"));

        // Should have both the argument/option parameters
        assertTrue("Should have name parameter", result.contains("#[Argument"));
        assertTrue("Should have yell parameter", result.contains("#[Option"));
    }

    public void testMigrationConvertsInvalidVariableNames() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Command;\n" +
                "use Symfony\\Component\\Console\\Command\\Command;\n" +
                "use Symfony\\Component\\Console\\Input\\InputArgument;\n" +
                "use Symfony\\Component\\Console\\Input\\InputInterface;\n" +
                "use Symfony\\Component\\Console\\Input\\InputOption;\n" +
                "use Symfony\\Component\\Console\\Output\\OutputInterface;\n" +
                "\n" +
                "class <caret>TestCommand extends Command\n" +
                "{\n" +
                "    protected function configure(): void\n" +
                "    {\n" +
                "        $this->addArgument('user-name', InputArgument::REQUIRED, 'User name');\n" +
                "        $this->addOption('dry-run', null, InputOption::VALUE_NONE, 'Dry run?');\n" +
                "        $this->addOption('max-count', null, InputOption::VALUE_OPTIONAL, 'Max count');\n" +
                "    }\n" +
                "\n" +
                "    protected function execute(InputInterface $input, OutputInterface $output): int\n" +
                "    {\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n"
        );

        // Apply the intention
        var intention = myFixture.findSingleIntention("Symfony: Migrate to invokable command");
        myFixture.launchAction(intention);

        String result = myFixture.getFile().getText();

        // Check that __invoke method exists
        assertTrue("Should have __invoke method", result.contains("public function __invoke("));

        // Check that invalid variable names are converted to camelCase
        assertTrue("Should convert user-name to userName", result.contains("$userName"));
        assertTrue("Should convert dry-run to dryRun", result.contains("$dryRun"));
        assertTrue("Should convert max-count to maxCount", result.contains("$maxCount"));

        // Check that name attribute is added for converted names
        assertTrue("Should have name attribute for user-name", result.contains("name: 'user-name'"));
        assertTrue("Should have name attribute for dry-run", result.contains("name: 'dry-run'"));
        assertTrue("Should have name attribute for max-count", result.contains("name: 'max-count'"));

        // Check that Argument and Option attributes are present
        assertTrue("Should have Argument attribute", result.contains("#[Argument("));
        assertTrue("Should have Option attribute", result.contains("#[Option("));
    }
}
