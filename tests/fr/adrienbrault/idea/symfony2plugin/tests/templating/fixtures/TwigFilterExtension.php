<?php

namespace{
    interface Twig_ExtensionInterface {}
    interface Twig_Environment {}
    abstract class Twig_Extension implements Twig_ExtensionInterface {}
    class Twig_SimpleFilter {}
    class SqlFormatter {
        public function format() {}
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
            new \Twig_SimpleFilter('json_decode', 'json_decode'),
        );
    }

    public function minifyQuery($query) {}
    public function contextAndEnvironment(\Twig_Environment $env, $context, $string) {}
    public function contextWithoutEnvironment($context, $string) {}

}