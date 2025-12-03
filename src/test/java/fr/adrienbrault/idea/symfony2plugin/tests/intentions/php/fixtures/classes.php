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

namespace Symfony\Component\Console\Attribute
{
    #[\Attribute(\Attribute::TARGET_CLASS)]
    class AsCommand
    {
        public function __construct(
            public ?string $name = null,
            public ?string $description = null,
            public array $aliases = [],
            public bool $hidden = false
        ) {}
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

namespace Symfony\Component\Console
{
    class Cursor
    {
    }

    class Application
    {
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

namespace Symfony\Bundle\FrameworkBundle\Controller
{
    class AbstractController
    {
    }
}

namespace Symfony\Component\Routing\Attribute
{
    #[\Attribute(\Attribute::TARGET_CLASS | \Attribute::TARGET_METHOD | \Attribute::IS_REPEATABLE)]
    class Route
    {
        public function __construct(
            ?string $path = null,
            ?string $name = null,
            array $requirements = [],
            array $options = [],
            array $defaults = [],
            ?string $host = null,
            array|string $methods = [],
            array|string $schemes = [],
            ?string $condition = null,
            ?int $priority = null,
            ?string $locale = null,
            ?string $format = null,
            ?bool $utf8 = null,
            ?bool $stateless = null,
            ?string $env = null
        ) {}
    }
}

namespace Symfony\Component\Routing\Annotation
{
    #[\Attribute(\Attribute::TARGET_CLASS | \Attribute::TARGET_METHOD | \Attribute::IS_REPEATABLE)]
    class Route
    {
        public function __construct(
            ?string $path = null,
            ?string $name = null
        ) {}
    }
}

namespace Symfony\Component\HttpKernel\Attribute
{
    #[\Attribute(\Attribute::TARGET_CLASS)]
    class AsController
    {
    }
}


