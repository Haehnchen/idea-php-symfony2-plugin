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
            ];
        }

        public function getTests()
        {
            return [
                new \Twig_SimpleTest('my_test', [$this, 'foobar']),
            ];
        }

        public function getFunctions()
        {
            return [
                new \Twig_SimpleFunction('max', 'max'),
                'form_enctype' => new \Twig_Function_Node('Symfony\Bridge\Twig\Node\FormEnctypeNode'),
                'hwi_oauth_login_url'  => new \Twig_Function_Method($this, 'foobar'),
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