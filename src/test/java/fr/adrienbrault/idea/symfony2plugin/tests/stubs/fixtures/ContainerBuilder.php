<?php

class Foo
{
    public function process(\Symfony\Component\DependencyInjection\ContainerBuilder $c)
    {
        $c->setParameter('container.builder.parameter');
    }

}