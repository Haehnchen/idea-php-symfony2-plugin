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

    public void testMigrationRemovesParentConstructorWithArguments() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Command;\n" +
                "use Symfony\\Component\\Console\\Command\\Command;\n" +
                "use Symfony\\Component\\Console\\Input\\InputInterface;\n" +
                "use Symfony\\Component\\Console\\Output\\OutputInterface;\n" +
                "\n" +
                "class <caret>TestCommand extends Command\n" +
                "{\n" +
                "    public function __construct()\n" +
                "    {\n" +
                "        parent::__construct('test');\n" +
                "        parent::__construct();\n" +
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

        // Check that extends Command is removed
        assertFalse("Should not extend Command", result.contains("extends Command"));

        // Check that parent::__construct('test') is removed
        assertFalse("Should not have any parent::__construct()", result.contains("parent::__construct("));

        // Constructor should still exist but without the parent call
        assertTrue("Should still have __construct method", result.contains("public function __construct()"));
    }

    public void testMigrationReplacesGetArgumentAndGetOptionWithDirectVariables() {
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
                "        $this->addArgument('age', InputArgument::OPTIONAL, 'The age');\n" +
                "        $this->addOption('verbose', 'v', InputOption::VALUE_NONE, 'Verbose output');\n" +
                "        $this->addOption('format', 'f', InputOption::VALUE_OPTIONAL, 'Output format');\n" +
                "    }\n" +
                "\n" +
                "    protected function execute(InputInterface $input, OutputInterface $output): int\n" +
                "    {\n" +
                "        $name = $input->getArgument('name');\n" +
                "        $age = $input->getArgument('age');\n" +
                "        $verbose = $input->getOption('verbose');\n" +
                "        $format = $input->getOption('format');\n" +
                "        if ($verbose) {\n" +
                "            $output->writeln('Name: ' . $name . ', Age: ' . $age . ', Format: ' . $format);\n" +
                "        }\n" +
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

        // Check that $input->getArgument() calls are replaced with direct variables
        assertFalse("Should not have $input->getArgument('name')", result.contains("$input->getArgument('name')"));
        assertFalse("Should not have $input->getArgument('age')", result.contains("$input->getArgument('age')"));

        // Check that $input->getOption() calls are replaced with direct variables
        assertFalse("Should not have $input->getOption('verbose')", result.contains("$input->getOption('verbose')"));
        assertFalse("Should not have $input->getOption('format')", result.contains("$input->getOption('format')"));

        // Check that redundant self-assignments are removed
        assertFalse("Should not have redundant $name = $name", result.contains("$name = $name"));
        assertFalse("Should not have redundant $age = $age", result.contains("$age = $age"));
        assertFalse("Should not have redundant $verbose = $verbose", result.contains("$verbose = $verbose"));
        assertFalse("Should not have redundant $format = $format", result.contains("$format = $format"));

        // Check that the parameters are still in the method signature with Argument/Option attributes
        assertTrue("Should have $name parameter with Argument attribute", result.contains("#[Argument") && result.contains("$name"));
        assertTrue("Should have $age parameter with Argument attribute", result.contains("$age"));
        assertTrue("Should have $verbose parameter with Option attribute", result.contains("#[Option") && result.contains("$verbose"));
        assertTrue("Should have $format parameter", result.contains("$format"));
    }

    public void testMigrationHandlesArgumentOptionNameAliasing() {
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
                "        $this->addArgument('no-dry-run', InputArgument::OPTIONAL, 'Save changes to DB');\n" +
                "        $this->addArgument('user-name', InputArgument::REQUIRED, 'User name');\n" +
                "        $this->addOption('dry-run', null, InputOption::VALUE_NONE, 'Dry run mode');\n" +
                "        $this->addOption('max-count', null, InputOption::VALUE_OPTIONAL, 'Maximum count');\n" +
                "    }\n" +
                "\n" +
                "    protected function execute(InputInterface $input, OutputInterface $output): int\n" +
                "    {\n" +
                "        $noDryRun = $input->getArgument('no-dry-run');\n" +
                "        $userName = $input->getArgument('user-name');\n" +
                "        $dryRun = $input->getOption('dry-run');\n" +
                "        $maxCount = $input->getOption('max-count');\n" +
                "        \n" +
                "        if ($dryRun) {\n" +
                "            $output->writeln('Dry run mode for user: ' . $userName);\n" +
                "        } elseif ($noDryRun) {\n" +
                "            $output->writeln('Saving changes for user: ' . $userName . ' (max: ' . $maxCount . ')');\n" +
                "        }\n" +
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

        // Check that argument/option names with hyphens have the 'name' attribute
        assertTrue("Should have name attribute for no-dry-run", result.contains("name: 'no-dry-run'"));
        assertTrue("Should have name attribute for user-name", result.contains("name: 'user-name'"));
        assertTrue("Should have name attribute for dry-run", result.contains("name: 'dry-run'"));
        assertTrue("Should have name attribute for max-count", result.contains("name: 'max-count'"));

        // Check that variable names are converted to camelCase
        assertTrue("Should have $noDryRun parameter", result.contains("$noDryRun"));
        assertTrue("Should have $userName parameter", result.contains("$userName"));
        assertTrue("Should have $dryRun parameter", result.contains("$dryRun"));
        assertTrue("Should have $maxCount parameter", result.contains("$maxCount"));

        // Check that $input->getArgument() and $input->getOption() calls are replaced correctly
        assertFalse("Should not have $input->getArgument('no-dry-run')", result.contains("$input->getArgument('no-dry-run')"));
        assertFalse("Should not have $input->getArgument('user-name')", result.contains("$input->getArgument('user-name')"));
        assertFalse("Should not have $input->getOption('dry-run')", result.contains("$input->getOption('dry-run')"));
        assertFalse("Should not have $input->getOption('max-count')", result.contains("$input->getOption('max-count')"));

        // Check that redundant self-assignments are removed
        assertFalse("Should not have redundant $noDryRun = $noDryRun", result.contains("$noDryRun = $noDryRun"));
        assertFalse("Should not have redundant $userName = $userName", result.contains("$userName = $userName"));
        assertFalse("Should not have redundant $dryRun = $dryRun", result.contains("$dryRun = $dryRun"));
        assertFalse("Should not have redundant $maxCount = $maxCount", result.contains("$maxCount = $maxCount"));

        // Verify the parameter definition has the correct format
        // e.g., #[Argument(name: 'no-dry-run', description: 'Save changes to DB')] ?string $noDryRun = null
        assertTrue("Should have proper Argument attribute for no-dry-run", result.contains("#[Argument(name: 'no-dry-run', description: 'Save changes to DB')]"));
        assertTrue("Should have proper Argument attribute for user-name", result.contains("#[Argument(name: 'user-name', description: 'User name')]"));
        assertTrue("Should have proper Option attribute for dry-run", result.contains("#[Option(name: 'dry-run', description: 'Dry run mode')]"));
        assertTrue("Should have proper Option attribute for max-count", result.contains("#[Option(name: 'max-count', description: 'Maximum count')]"));
    }

    public void testMigrationRemovesTypeCastFromGetArgument() {
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
                "        $this->addArgument('count', InputArgument::REQUIRED, 'The count');\n" +
                "        $this->addArgument('price', InputArgument::REQUIRED, 'The price');\n" +
                "    }\n" +
                "\n" +
                "    protected function execute(InputInterface $input, OutputInterface $output): int\n" +
                "    {\n" +
                "        $count = (int) $input->getArgument('count');\n" +
                "        $price = (float) $input->getArgument('price');\n" +
                "        $total = $count * $price;\n" +
                "        $output->writeln('Total: ' . $total);\n" +
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

        // Check that type casts and method calls are removed
        assertFalse("Should not have (int) cast", result.contains("(int) $input->getArgument('count')"));
        assertFalse("Should not have (float) cast", result.contains("(float) $input->getArgument('price')"));
        assertFalse("Should not have $input->getArgument('count')", result.contains("$input->getArgument('count')"));
        assertFalse("Should not have $input->getArgument('price')", result.contains("$input->getArgument('price')"));

        // Check that redundant self-assignments are removed
        assertFalse("Should not have redundant $count = $count", result.contains("$count = $count"));
        assertFalse("Should not have redundant $price = $price", result.contains("$price = $price"));

        // Check that the parameters are in the method signature with Argument attributes
        assertTrue("Should have $count parameter", result.contains("$count"));
        assertTrue("Should have $price parameter", result.contains("$price"));
        assertTrue("Should have Argument attributes", result.contains("#[Argument"));
    }
}
