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
    }
}
