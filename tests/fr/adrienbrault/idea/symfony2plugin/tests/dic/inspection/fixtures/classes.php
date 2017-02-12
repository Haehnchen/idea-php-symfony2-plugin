<?php

namespace Symfony\Component\DependencyInjection
{
    interface ContainerInterface
    {
        public function get();
    };
}

namespace Foobar
{
    class Car
    {
        const FOOBAR = null;
    }
}