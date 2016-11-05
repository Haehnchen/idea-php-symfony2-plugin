<?php

namespace
{
    define('CONST_FOO', 'CONST_FOO');
}

namespace Foo
{
    class Bar
    {
        const FOO = 'foo';

        public function create() {}
    }
}