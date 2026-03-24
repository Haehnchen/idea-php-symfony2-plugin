<?php

namespace Symfony\Component\Console\Command
{
    class Command
    {
        public function addOption($name, $shortcut = null, $mode = null, $description = '', $default = null) {}
        public function addArgument($name, $mode = null, $description = '', $default = null) {}
        public function setDefinition($definition) {}
        public function setName($name) {}
    }
}

namespace Symfony\Component\Console\Attribute
{
    class AsCommand {}
    class Option {}
    class Argument {}
}

namespace Symfony\Component\Console\Input
{
    class InputOption
    {
        const VALUE_NONE = 1;
        const VALUE_REQUIRED = 2;
        const VALUE_OPTIONAL = 4;
    }
    class InputArgument
    {
        const REQUIRED = 1;
        const OPTIONAL = 2;
    }
    class InputInterface {}
}

namespace Symfony\Component\Console\Output
{
    class OutputInterface {}
}

namespace Symfony\Component\Console\Style
{
    class SymfonyStyle {}
}

/**
 * Traditional command with addOption() calls.
 * Extending Command means it is found via subclass scan (not attribute index), so multiple
 * such classes can safely share a file.
 */
namespace TerminalFixtures
{
    use Symfony\Component\Console\Attribute\AsCommand;
    use Symfony\Component\Console\Attribute\Option;
    use Symfony\Component\Console\Attribute\Argument;
    use Symfony\Component\Console\Command\Command;
    use Symfony\Component\Console\Input\InputArgument;
    use Symfony\Component\Console\Input\InputInterface;
    use Symfony\Component\Console\Input\InputOption;
    use Symfony\Component\Console\Output\OutputInterface;
    use Symfony\Component\Console\Style\SymfonyStyle;

    #[AsCommand(name: 'terminal:with-options')]
    class CommandWithOptions extends Command
    {
        protected function configure(): void
        {
            $this
                ->addOption('env', 'e', InputOption::VALUE_REQUIRED, 'The environment')
                ->addOption('no-debug', null, InputOption::VALUE_NONE, 'Disable debug mode');
        }

        public function execute(InputInterface $input, OutputInterface $output): int
        {
            return Command::SUCCESS;
        }
    }

    #[AsCommand(name: 'terminal:with-args')]
    class CommandWithArgs extends Command
    {
        protected function configure(): void
        {
            $this
                ->addArgument('username', InputArgument::REQUIRED, 'The username')
                ->addArgument('role', InputArgument::OPTIONAL, 'The role', 'ROLE_USER');
        }

        public function execute(InputInterface $input, OutputInterface $output): int
        {
            return Command::SUCCESS;
        }
    }

    #[AsCommand(name: 'terminal:with-definition')]
    class CommandWithDefinition extends Command
    {
        protected function configure(): void
        {
            $this->setDefinition([
                new InputOption('format', 'f', InputOption::VALUE_REQUIRED, 'Output format'),
                new InputOption('verbose', 'v', InputOption::VALUE_NONE, 'Verbose output'),
            ]);
        }

        public function execute(InputInterface $input, OutputInterface $output): int
        {
            return Command::SUCCESS;
        }
    }

    #[AsCommand(name: 'terminal:mixed')]
    class MixedCommand extends Command
    {
        protected function configure(): void
        {
            $this->addOption('config', 'c', InputOption::VALUE_REQUIRED, 'Config file');
        }

        public function __invoke(
            SymfonyStyle $io,
            #[Option(description: 'Force execution')] bool $force = false,
        ): int
        {
            return 0;
        }
    }

    #[AsCommand(name: 'terminal:mixed-args')]
    class MixedArgsCommand extends Command
    {
        protected function configure(): void
        {
            $this->addArgument('target', InputArgument::REQUIRED, 'Target path');
        }

        public function __invoke(
            SymfonyStyle $io,
            #[Argument(name: 'target', description: 'Target path')] string $target,
            #[Argument(description: 'Source path')] ?string $source = null,
        ): int
        {
            return 0;
        }
    }
}
