<?php


namespace
{
    define('CONST_FOO', 'CONST_FOO');

    class DateTime
    {
        public function format() {}
    }
}

namespace Symfony\Component\HttpKernel\Bundle
{
    interface Bundle {}
}

namespace FooBundle
{
    use Symfony\Component\HttpKernel\Bundle\Bundle;
    class FooBundle implements Bundle {}
}

namespace FooBundle\Controller
{
    class FooController
    {
        public function barAction($slug) {}
    }
}

namespace Foo\ConstantBar
{
    class Foo
    {
        const FOO = 'BAR';
    }
}


namespace Foo\Template {

    class Foobar
    {
        /**
         * @var \DateTime[]
         */
        public $items;

        public bool $ready;

        public function __construct(public array $myfoos)
        {
        }

        public function getFoobar(): array
        {
        }

        /**
         * @return \DateTime[]
         */
        public function getDates()
        {
        }

        /**
         * @return bool
         */
        public function isReadyStatus()
        {
        }
    }
}

namespace Symfony\Component\HttpFoundation
{
    class Request
    {
        public function isMethod(string $method) {}
    }
}
