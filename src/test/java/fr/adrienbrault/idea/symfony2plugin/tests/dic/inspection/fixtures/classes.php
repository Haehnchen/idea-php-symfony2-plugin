<?php

namespace Symfony\Component\DependencyInjection
{
    interface ContainerInterface
    {
        public function get();
    };
}

namespace Psr\Container
{
    interface ContainerInterface
    {
        public function get(string $id);
        public function has(string $id);
    }
}

namespace Symfony\Component\DependencyInjection\ParameterBag
{
    interface ContainerBagInterface extends \Psr\Container\ContainerInterface
    {
        public function all();
    }
}

namespace Foobar
{
    class Car
    {
        const FOOBAR = null;
    }

    class NamedArgument
    {
        public function __construct($foobar)
        {
        }
    }
}

namespace Symfony\Component\DependencyInjection\Attribute
{
    class Autowire {}
    class AsDecorator {}
}
