<?php

namespace Foo;

use Symfony\Component\DependencyInjection\ContainerBuilder;
use Symfony\Component\DependencyInjection\Container;
use Symfony\Component\DependencyInjection\TaggedContainerInterface;

class Foo
{
    public function process(ContainerBuilder $container2)
    {
        $container2->findTaggedServiceIds('TaggedServiceIds');
        $container2->setParameter('parameter');
        $container2->setDefinition('definition');
        $container2->setAlias('alias');
        $container2->register('register.id', 'Register');
    }

    public function process2(Container $container2)
    {
        $container2->findTaggedServiceIds('TaggedServiceIds2');
    }

    public function process3(TaggedContainerInterface $foo)
    {
        $foo->findTaggedServiceIds('TaggedServiceIds4');
    }

    public function process4($car, $apple, Container $foo)
    {
        $apple->setDefinition('apple');
        $foo->setDefinition('definition3');
    }
}