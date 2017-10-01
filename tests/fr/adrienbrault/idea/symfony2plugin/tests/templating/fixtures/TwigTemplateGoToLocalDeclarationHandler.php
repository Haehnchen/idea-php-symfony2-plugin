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
    use Twig\TokenParser\TokenParserInterface;

    class Extensions implements \Twig_ExtensionInterface, TokenParserInterface {
        public function getFunctions()
        {
            return [
                new \Twig_SimpleFunction('foo_test', 'foo_test'),
            ];
        }

        public function getTag() {
            return 'tag_foobar';
        }
    }
}

namespace Twig\TokenParser
{
    interface TokenParserInterface
    {
        public function getTag();
    }
}