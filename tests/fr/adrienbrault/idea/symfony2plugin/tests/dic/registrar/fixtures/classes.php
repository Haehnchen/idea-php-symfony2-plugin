<?php

namespace Symfony\Component\DependencyInjection
{
    interface ContainerInterface
    {
        function getParameter($parameter);
        function hasParameter($parameter);
    }
}

namespace
{
    class Foo
    {

        /**
         * @var \Symfony\Component\DependencyInjection\ContainerInterface
         */
        private $container;

        public function foo($parameter) {
            return $this->container->getParameter($parameter);
        }

        public function bar($parameter) {
            return $this->container->hasParameter($parameter);
        }
    }
}


