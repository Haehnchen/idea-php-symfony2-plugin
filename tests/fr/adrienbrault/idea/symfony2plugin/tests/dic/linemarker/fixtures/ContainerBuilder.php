<?php

namespace Foo;

use Symfony\Component\DependencyInjection\ContainerBuilder;

class Foo
{
    public function process(ContainerBuilder $container)
    {
        $container->getDefinition('foo_bar_main');
    }
}