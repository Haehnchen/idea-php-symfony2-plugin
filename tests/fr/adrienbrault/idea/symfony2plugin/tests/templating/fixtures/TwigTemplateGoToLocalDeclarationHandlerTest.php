<?php

namespace
{
    function foo_test() {}

    interface Twig_ExtensionInterface
    {
        public function getFunctions();
    }

    class Twig_SimpleFunction
    {
    }
}

namespace Twig
{
    class Extensions implements \Twig_ExtensionInterface {
        public function getFunctions()
        {
            return [
                new \Twig_SimpleFunction('foo_test', 'foo_test'),
            ];
        }
    }
}