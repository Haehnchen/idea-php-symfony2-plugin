<?php

namespace
{
    /**
     * @param string $foobar
     */
    function a_test($foobar) {}

    /**
     * @param string $foobar3
     */
    function b_test($foobar, $foobar2, $foobar3) {}

    function c_test(string $foobar3) {}

    function d_test($foobar3) {}

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
                new \Twig_SimpleFunction('a_test', 'a_test'),
                new \Twig_SimpleFunction('b_test', 'b_test', array('needs_context' => true, 'needs_environment' => true)),
                new \Twig_SimpleFunction('c_test', 'c_test'),
                new \Twig_SimpleFunction('d_test', 'd_test'),
            ];
        }
    }
}