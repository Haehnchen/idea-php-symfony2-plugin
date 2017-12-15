<?php

namespace
{
    function foo_test() {}
    class My_Node_Test {}

    interface Twig_ExtensionInterface
    {
        public function getTokenParsers();
        public function getNodeVisitors();
        public function getFilters();
        public function getTests();
        public function getFunctions();
        public function getOperators();
        public function getGlobals();
        public function getName();
    }

    class Twig_SimpleFilter
    {
    }

    class Twig_SimpleFunction
    {
    }

    class Twig_SimpleTest
    {
    }

    class Twig_Function_Node
    {
    }

    class Twig_Function_Method
    {
    }

    class Twig_Filter_Method
    {
    }

    class Twig_Filter_Function
    {
    }
}

namespace Twig
{
    class Extensions implements \Twig_ExtensionInterface {
        public function getFilters()
        {
            return [
                new \Twig_SimpleFilter('foobar', [$this, 'foobar']),
            ];
        }

        public function foobar()
        {
        }
    }
}