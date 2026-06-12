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
