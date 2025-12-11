<?php

namespace Symfony\Component\Console\Command
{
    class Command
    {
        function setName($i) {}
    }
}

namespace Foo
{
    use Symfony\Component\Console\Attribute\AsCommand;
    use Symfony\Component\Console\Command\Command;

    class FooCommand extends Command
    {
        public function configure()
        {
            $this->setName('foo');
        }
    }

    class PropertyCommand extends Command
    {
        private $name = 'property';

        public function configure()
        {
            $this->setName($this->name);
        }
    }

    class ConstCommand extends Command
    {
        const FOO = 'const';

        public function configure()
        {
            $this->setName(self::FOO);
        }
    }

    #[AsCommand('app:create-user-1')]
    class FoobarCommand1  extends Command {}

    #[AsCommand(name: 'app:create-user-2')]
    class FoobarCommand2  extends Command {}

    class FoobarCommand3 extends Command
    {
        protected static $defaultName = 'app:create-user-3';
    }
}

namespace Symfony\Component\Console\Attribute
{
    class AsCommand {}
    class Option {}
}

namespace Symfony\Component\Console\Input\Attribute
{
    class Option {}
}

namespace Symfony\Component\Console\Input
{
    class InputInterface {}
    class InputOption {}
}

namespace Symfony\Component\Console\Output
{
    class OutputInterface {}
}

namespace Symfony\Component\Console\Style
{
    class SymfonyStyle {}
}

namespace CommandOptions
{
    use Symfony\Component\Console\Attribute\AsCommand;
    use Symfony\Component\Console\Attribute\Option;
    use Symfony\Component\Console\Command\Command;
    use Symfony\Component\Console\Input\InputInterface;
    use Symfony\Component\Console\Input\InputOption;
    use Symfony\Component\Console\Output\OutputInterface;
    use Symfony\Component\Console\Style\SymfonyStyle;

    // Traditional command with addOption() calls
    #[AsCommand(name: 'app:traditional')]
    class TraditionalCommand extends Command
    {
        protected function configure(): void
        {
            $this
                ->addOption('name', null, InputOption::VALUE_NONE)
                ->addOption('last_name', 'd', InputOption::VALUE_NONE);
        }

        public function execute(InputInterface $input, OutputInterface $output): int
        {
            return Command::SUCCESS;
        }
    }

    // Modern invokable command with #[Option] attributes
    #[AsCommand(name: 'app:modern')]
    class ModernCommand
    {
        public function __invoke(
            SymfonyStyle $io,
            #[Option(name: 'idle')] ?int $timeout = null,
            #[Option] string $type = 'USER_TYPE',
            #[Option(shortcut: 'v')] bool $verbose = false,
            #[Option(description: 'User groups')] array $groups = [],
        ): int
        {
            return Command::SUCCESS;
        }
    }

    // Mixed: traditional configure + invokable
    #[AsCommand(name: 'app:mixed')]
    class MixedCommand extends Command
    {
        protected function configure(): void
        {
            $this->addOption('config', 'c', InputOption::VALUE_REQUIRED);
        }

        public function __invoke(
            SymfonyStyle $io,
            #[Option] bool $force = false,
        ): int
        {
            return Command::SUCCESS;
        }
    }

    // Using setDefinition() with new InputOption()
    #[AsCommand(name: 'app:definition')]
    class DefinitionCommand extends Command
    {
        protected function configure(): void
        {
            $this->setDefinition([
                new InputOption('format', 'f', InputOption::VALUE_REQUIRED),
                new InputOption('verbose', 'v', InputOption::VALUE_NONE),
            ]);
        }

        public function execute(InputInterface $input, OutputInterface $output): int
        {
            return Command::SUCCESS;
        }
    }

    // Using setDefinition() with single InputOption
    #[AsCommand(name: 'app:definition-single')]
    class DefinitionSingleCommand extends Command
    {
        protected function configure(): void
        {
            $this->setDefinition(
                new InputOption('debug', 'd', InputOption::VALUE_NONE)
            );
        }

        public function execute(InputInterface $input, OutputInterface $output): int
        {
            return Command::SUCCESS;
        }
    }
}

namespace Symfony\Component\Console\Input
{
    class InputArgument {}
}

namespace Symfony\Component\Console\Attribute
{
    class Argument {}
}

namespace CommandArguments
{
    use Symfony\Component\Console\Attribute\AsCommand;
    use Symfony\Component\Console\Attribute\Argument;
    use Symfony\Component\Console\Command\Command;
    use Symfony\Component\Console\Input\InputArgument;
    use Symfony\Component\Console\Input\InputInterface;
    use Symfony\Component\Console\Output\OutputInterface;
    use Symfony\Component\Console\Style\SymfonyStyle;

    // Traditional command with addArgument() calls
    #[AsCommand(name: 'app:traditional-args')]
    class TraditionalArgsCommand extends Command
    {
        protected function configure(): void
        {
            $this
                ->addArgument('username', InputArgument::REQUIRED, 'The username')
                ->addArgument('password', InputArgument::OPTIONAL, 'The password', 'default123');
        }

        public function execute(InputInterface $input, OutputInterface $output): int
        {
            return Command::SUCCESS;
        }
    }

    // Modern invokable command with #[Argument] attributes
    #[AsCommand(name: 'app:modern-args')]
    class ModernArgsCommand
    {
        public function __invoke(
            SymfonyStyle $io,
            #[Argument(description: 'The user name')] string $name,
            #[Argument(name: 'user-id', description: 'The user ID')] int $userId,
            #[Argument] ?string $email = null,
        ): int
        {
            return Command::SUCCESS;
        }
    }

    // Using setDefinition() with new InputArgument()
    #[AsCommand(name: 'app:definition-args')]
    class DefinitionArgsCommand extends Command
    {
        protected function configure(): void
        {
            $this->setDefinition([
                new InputArgument('source', InputArgument::REQUIRED, 'Source file'),
                new InputArgument('destination', InputArgument::OPTIONAL, 'Destination file', '/tmp/default'),
            ]);
        }

        public function execute(InputInterface $input, OutputInterface $output): int
        {
            return Command::SUCCESS;
        }
    }

    // Mixed: traditional configure + invokable with arguments
    #[AsCommand(name: 'app:mixed-args')]
    class MixedArgsCommand extends Command
    {
        protected function configure(): void
        {
            $this->addArgument('config-file', InputArgument::REQUIRED, 'Config file path');
        }

        public function __invoke(
            SymfonyStyle $io,
            #[Argument(name: 'config-file', description: 'Config file path')] string $configFile,
            #[Argument] bool $force = false,
        ): int
        {
            return Command::SUCCESS;
        }
    }
}