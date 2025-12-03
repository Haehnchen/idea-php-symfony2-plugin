<?php

namespace Symfony\Component\Console\Command
{
    class Command
    {
        public const SUCCESS = 0;
        public const FAILURE = 1;
        public const INVALID = 2;
    }
}

namespace Symfony\Component\Console\Attribute
{

    /**
     * Service tag to autoconfigure commands.
     */
    #[\Attribute(\Attribute::TARGET_CLASS)]
    class AsCommand
    {
        public function __construct(
            public string  $name,
            public ?string $description = null,
            array          $aliases = [],
            bool           $hidden = false,
        ) {}
    }
}
