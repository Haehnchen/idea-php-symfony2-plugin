<?php

namespace Foo {
    interface BarInterface
    {
    }

    class Bar
    {
        public function __construct($foobar)
        {
        }
    }

    class Foo
    {
    }

    class MultiMatchBar
    {
        public function __construct(Bar|Foo $foo)
        {
        }
    }

    class MultiMatchXml
    {
        public function __construct(Bar|Foo $foo)
        {
        }
    }

    class MultiMatchFluent
    {
        public function __construct(Bar|Foo $foo)
        {
        }
    }

    class MultiMatchWithUnknownSecond
    {
        public function __construct(Bar|Foo $foo, UnknownType $unknown)
        {
        }
    }

    class Baz
    {
    }

    class BazBlank
    {
    }
}