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

    // Twig 2.0
    class Twig_Filter
    {
    }

    class Twig_Test
    {
    }

    class Twig_Function
    {
    }
}

namespace Twig
{
    // Twig 3.0
    class TwigFilter {}
    class TwigFunction {}
    class TwigTest {}

    class Extensions implements \Twig_ExtensionInterface {
        public function getTokenParsers()
        {
        }

        public function getNodeVisitors()
        {
        }

        public function getFilters()
        {
            return [
                new \Twig_SimpleFilter('trans', [$this, 'foobar']),
                'doctrine_minify_query' => new \Twig_Filter_Method($this, 'foobar'),
                'localizeddate' => new \Twig_Filter_Function('foobar'),
                new \Twig_Filter('trans_2', [$this, 'foobar']),
                new TwigFilter('trans_3', [$this, 'foobar']),
            ];
        }

        public function getTests()
        {
            return [
                new \Twig_SimpleTest('my_test', null, array('node_class' => 'My_Node_Test')),
                new \Twig_SimpleTest('my_test_2', 'foo_test'),
                new \Twig_Test('iterable_2', 'foo_test'),
                new TwigTest('iterable_3', 'foo_test'),
            ];
        }

        public function getFunctions()
        {
            return [
                new \Twig_SimpleFunction('max', 'max'),
                'form_enctype' => new \Twig_Function_Node('Symfony\Bridge\Twig\Node\FormEnctypeNode'),
                'hwi_oauth_login_url'  => new \Twig_Function_Method($this, 'foobar'),
                new \Twig_Function('max_2', 'max'),
                new TwigFunction('max_3', 'max'),
            ];
        }

        public function getOperators()
        {
            return [
                [
                    'not' => ['precedence' => 50, 'class' => 'Twig_Node_Expression_Unary_Not'],
                ],
                [
                    'or' => ['precedence' => 10, 'class' => 'Twig_Node_Expression_Binary_Or'],
                ],
            ];
        }

        public function getGlobals()
        {
        }

        public function getName()
        {
        }

        public function foobar()
        {
        }
    }
}