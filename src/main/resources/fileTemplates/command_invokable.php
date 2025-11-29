<?php

declare(strict_types=1);

namespace {{ namespace }};

use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Style\SymfonyStyle;

#[AsCommand(name: '{{ command_name }}', description: 'Hello PhpStorm')]
class {{ class }}
{
    public function __invoke(SymfonyStyle $io): int
    {
        return Command::SUCCESS;
    }
}
