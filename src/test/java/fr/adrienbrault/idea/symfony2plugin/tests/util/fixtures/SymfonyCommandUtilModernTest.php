<?php

namespace Symfony\Component\Console\Attribute
{
    class AsCommand {}
}

namespace SymfonyCommandUtilModern
{
    use Symfony\Component\Console\Attribute\AsCommand;

    /**
     * Modern invokable command: has #[AsCommand] but does NOT extend Command.
     * Must be in its own file so PhpAttributeIndex stores it (index keeps one entry per file per key).
     */
    #[AsCommand(name: 'app:modern-invokable')]
    class ModernInvokableCommand
    {
        public function __invoke(): int
        {
            return 0;
        }
    }
}
