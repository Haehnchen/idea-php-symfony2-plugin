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

    final class TwigFilter
    {
        public function __construct(string $name, $callable = null, array $options = [])
        {
        }
    }

    final class TwigFunction
    {
        public function __construct(string $name, $callable = null, array $options = [])
        {
        }
    }
    class Environment
    {
    }

}

namespace
{
    use Twig\Environment;

    function request_filter(Environment $environment, $value, $parameter)
    {
        /** @var \Symfony\Component\HttpFoundation\Request $x  */
        $x->isMethod($parameter);
    }

    function request_function(Environment $environment, $parameter)
    {
        /** @var \Symfony\Component\HttpFoundation\Request $x  */
        $x->isMethod($parameter);
    }
}


namespace Twig\Extension
{

    use Twig\TwigFilter;
    use Twig\TwigFunction;

    interface ExtensionInterface
    {
    }

    class FooExtensions implements ExtensionInterface {
        public function getFilters(): array
        {
            return [
                new TwigFilter('request_filter', 'request_filter', ['needs_environment' => true]),
            ];
        }

        public function getFunctions(): array
        {
            return [
                new TwigFunction('request_function', 'request_function', ['needs_environment' => true]),
            ];
        }
    }
}
