<?php

declare(strict_types=1);

namespace {{ namespace }};

use Twig\Attribute\AsTwigFunction;
use Twig\Attribute\AsTwigFilter;

class {{ class }}
{
    #[AsTwigFilter('my_filter')]
    public function myFilter(): string
    {
        return 'Hello from PhpStorm';
    }

    #[AsTwigFunction('my_function')]
    public function myFunction(): string
    {
        return 'Hello from PhpStorm';
    }
}