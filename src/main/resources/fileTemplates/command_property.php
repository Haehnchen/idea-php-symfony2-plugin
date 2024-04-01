<?php

declare(strict_types=1);

namespace {{ namespace }};

use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;

class {{ class }} extends Command
{
    protected static $defaultName = '{{ command_name }}';
    protected static $defaultDescription = 'Hello PhpStorm';

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        return Command::SUCCESS;
    }
}
