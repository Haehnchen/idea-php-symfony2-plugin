<?php

namespace{
    function my_json_decode() {}
    function twig_test_even() {}

    interface Twig_ExtensionInterface {}
    interface Twig_Environment {}
    abstract class Twig_Extension implements Twig_ExtensionInterface {}
    class Twig_SimpleFilter {}
    class Twig_SimpleFunction {}
    class Twig_SimpleTest {}
    class SqlFormatter {
        public function format() {}
    }

    interface Twig_TokenParserInterface {}

    class FooTokenParser implements Twig_TokenParserInterface
    {
        public function getTag() { return 'foo_tag'; }
    }
}

namespace Doctrine\Bundle\DoctrineBundle\Twig;

class DoctrineExtension extends \Twig_Extension
{

    public function getFilters()
    {
        return array(
            new \Twig_SimpleFilter('doctrine_minify_query', array($this, 'minifyQuery')),
            new \Twig_SimpleFilter('doctrine_pretty_query', 'SqlFormatter::format'),
            new \Twig_SimpleFilter('contextAndEnvironment', array($this, 'minifyQuery'), array('needs_context' => true, 'needs_environment' => true)),
            new \Twig_SimpleFilter('contextWithoutEnvironment', array($this, 'minifyQuery'), array('needs_environment' => true)),
            new \Twig_SimpleFilter('json_decode', 'my_json_decode'),
        );
    }

    public function getFunctions()
    {
        return array(
            new \Twig_SimpleFunction('foobar', array($this, 'foobar')),
            new \Twig_SimpleFunction('json_bar', 'my_json_decode'),
        );
    }

    public function getTests()
    {
        return array(
            new \Twig_SimpleTest('bar_even', 'twig_test_even'),
            new \Twig_SimpleTest('bar even', 'twig_test_even'),
        );
    }

    public function getOperators()
    {
        return array(
            array(
                'not' => array(),
                '-' => array(),
            ),
            array(
                'or' => array(),
                'b-or' => array(),
                'b-xor' => array(),
                'starts with' => array(),
                'ends with' => array(),
                '**' => array(),
            ),
        );
    }

    public function minifyQuery($query) {}
    public function foobar() {}
    public function contextAndEnvironment(\Twig_Environment $env, $context, $string) {}
    public function contextWithoutEnvironment($context, $string) {}

}