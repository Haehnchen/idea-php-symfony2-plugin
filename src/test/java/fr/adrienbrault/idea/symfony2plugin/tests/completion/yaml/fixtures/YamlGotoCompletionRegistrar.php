<?php

namespace Foo
{
    class Bar
    {
        public const BAZ = 'baz';

        public function create() {}
    }

    class Foobar extends Bar
    {
        public function __construct($arg = null) {}
    }
}