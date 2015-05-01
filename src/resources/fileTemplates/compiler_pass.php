<?php

namespace {{ ns }};

use Symfony\Component\DependencyInjection\ContainerBuilder;
use Symfony\Component\DependencyInjection\Compiler\CompilerPassInterface;

class {{ class }} implements CompilerPassInterface
{
    public function process(ContainerBuilder $container)
    {

    }
}
