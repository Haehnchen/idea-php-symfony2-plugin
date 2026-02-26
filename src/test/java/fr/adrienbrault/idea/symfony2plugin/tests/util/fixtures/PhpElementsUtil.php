<?php

namespace Foo {
    class Bar {}
    interface BarInterface {}
}

namespace FooBar
{
    class Foo
    {
        public function getBar()
        {
        }
    }

    class Bar extends Foo
    {
        public function getBar()
        {
        }
    }

    class Car extends Bar
    {
    }
}