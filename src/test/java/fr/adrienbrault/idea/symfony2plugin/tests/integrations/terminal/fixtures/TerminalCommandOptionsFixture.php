<?php

namespace Symfony\Component\Console\Attribute
{
    class AsCommand {}
    class Option {}
}

namespace Symfony\Component\Console\Style
{
    class SymfonyStyle {}
}

/**
 * Standalone invokable command with #[Option] attributes only (no extends Command).
 * Must be in its own file so PhpAttributeIndex stores a separate entry for it.
 */
namespace TerminalFixtures
{
    use Symfony\Component\Console\Attribute\AsCommand;
    use Symfony\Component\Console\Attribute\Option;
    use Symfony\Component\Console\Style\SymfonyStyle;

    #[AsCommand(name: 'terminal:modern-options')]
    class ModernOptionsCommand
    {
        public function __invoke(
            SymfonyStyle $io,
            #[Option(name: 'idle')] ?int $timeout = null,
            #[Option(shortcut: 'v', description: 'Enable verbose output')] bool $verbose = false,
        ): int
        {
            return 0;
        }
    }
}
