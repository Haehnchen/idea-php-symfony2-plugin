<?php

namespace Symfony\Component\Console\Attribute
{
    class AsCommand {}
    class Argument {}
}

namespace Symfony\Component\Console\Style
{
    class SymfonyStyle {}
}

/**
 * Standalone invokable command with #[Argument] attributes only (no extends Command).
 * Must be in its own file so PhpAttributeIndex stores a separate entry for it.
 */
namespace TerminalFixtures
{
    use Symfony\Component\Console\Attribute\AsCommand;
    use Symfony\Component\Console\Attribute\Argument;
    use Symfony\Component\Console\Style\SymfonyStyle;

    #[AsCommand(name: 'terminal:modern-args')]
    class ModernArgsCommand
    {
        public function __invoke(
            SymfonyStyle $io,
            #[Argument(description: 'The username')] string $username,
            #[Argument(name: 'user-id', description: 'The user ID')] int $userId,
        ): int
        {
            return 0;
        }
    }
}
