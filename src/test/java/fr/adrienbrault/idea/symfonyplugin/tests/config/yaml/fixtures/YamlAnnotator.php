<?php

namespace Args
{
    class Foo
    {
        public function __construct(\Args\Foo $foo, $bar, \Args\Foo $car)
        {
        }

        public function setFoo(\Args\Foo $foo, $bar, \Args\Foo $car)
        {
        }
    }

    class Bar {}
}