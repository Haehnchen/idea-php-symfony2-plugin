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

namespace Symfony\Component\Console\Attribute
{
    #[\Attribute(\Attribute::TARGET_CLASS)]
    class AsCommand
    {
        public function __construct(
            public ?string $name = null,
            public ?string $description = null,
            public array $aliases = []
        ) {}
    }
}
