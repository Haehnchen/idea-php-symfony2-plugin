<?php

namespace Symfony\Component\Console\Command
{
    class Command
    {
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
