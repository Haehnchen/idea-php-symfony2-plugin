<?php

namespace Symfony\Component\Console\Command
{
    class Command
    {
    }

    class InvokableCommand
    {

    }
}

namespace Symfony\Component\Console\Input
{
    interface InputInterface
    {
        public function getArgument($name);
        public function getOption($name);
    }
}

namespace Symfony\Component\Console\Output
{
    interface OutputInterface
    {
        public function writeln($messages);
    }
}

namespace Symfony\Component\Console\Style
{
    class SymfonyStyle
    {
        public function writeln($message);
        public function success($message);
        public function error($message);
    }
}

namespace Symfony\Component\Console\Input\Attribute
{
    #[\Attribute(\Attribute::TARGET_PARAMETER)]
    class Argument
    {
        public function __construct(
            public ?string $description = null
        ) {}
    }

    #[\Attribute(\Attribute::TARGET_PARAMETER)]
    class Option
    {
        public function __construct(
            public ?string $shortcut = null,
            public ?string $description = null
        ) {}
    }
}


