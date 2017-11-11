<?php

namespace Foo {
    class Bar {}
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