<?php

namespace App\Twig;

use Twig\Attribute\AsTwigFilter;
use Twig\Attribute\AsTwigFunction;
use Twig\Attribute\AsTwigTest;

class AppExtension
{
    #[AsTwigFilter('product_number_filter')]
    public function formatProductNumberFilter(string $number): string
    {
    }

    #[AsTwigFunction('product_number_function')]
    public function formatProductNumberFunction(string $number): string
    {
    }

    #[AsTwigTest('product_number_test')]
    public function formatProductNumberTest(string $number): string
    {
    }
}

namespace App\Command;

use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;

#[AsCommand(
    name: 'app:create-user',
    aliases: ['app:create-admin']
)]
class CreateUserCommand extends Command
{
}
