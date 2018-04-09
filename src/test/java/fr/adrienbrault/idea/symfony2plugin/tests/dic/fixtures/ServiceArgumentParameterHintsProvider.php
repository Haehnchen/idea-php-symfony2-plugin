<?php

namespace Foobar
{
    interface FooInterface
    {
    }

    class MyFoobar
    {
        public function __construct(FooInterface $foo, $foobar)
        {
        }

        public function setFoo(FooInterface $foo)
        {
        }
    }

    class MyFoobarNamed
    {
        public function __construct($foobar, $foobar2, FooInterface $foo)
        {
        }
    }
}
