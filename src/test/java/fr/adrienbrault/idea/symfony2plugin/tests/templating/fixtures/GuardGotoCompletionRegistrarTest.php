<?php

namespace
{
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
}

namespace Twig
{
    class TestExtension implements \Twig_ExtensionInterface
    {
        public function getFunctions()
        {
            return [
                new \Twig_SimpleFunction('test_function', [$this, 'test_function']),
            ];
        }

        public function getFilters()
        {
            return [
                new \Twig_SimpleFilter('test_filter', [$this, 'test_filter']),
            ];
        }

        public function getTests()
        {
            return [
                new \Twig_SimpleTest('test_test', [$this, 'test_test']),
            ];
        }

        public function test_function()
        {
        }

        public function test_filter()
        {
        }

        public function test_test()
        {
        }
    }
}
