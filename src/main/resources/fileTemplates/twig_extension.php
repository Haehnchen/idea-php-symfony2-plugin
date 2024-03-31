<?php

declare(strict_types=1);

namespace {{ namespace }};

use Twig\Extension\AbstractExtension;
use Twig\TwigFunction;
use Twig\TwigFilter;

class {{ class }} extends AbstractExtension
{
    public function getFunctions(): array
    {
        return [
            new TwigFunction('my_function', $this->myFunction(...)),
        ];
    }

    public function getFilters(): array
    {
        return [
            new TwigFilter('my_filter', $this->myFunction(...)),
        ];
    }

    public function myFunction(): string
    {
        return 'Hello PhpStorm';
    }
}
