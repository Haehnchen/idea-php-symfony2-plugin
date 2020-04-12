<?php

namespace NamedArgument
{
    class Foobar
    {
        public function __construct(Foobar $foobar)
        {
        }

        public function setFoo($foo, Foobar $foobar)
        {
        }
    }
}

namespace App\Controller
{
    class FoobarController
    {
        public function fooAction($fooEntity, string $foobarString)
        {
        }

        private function fooPrivate($private)
        {
        }
    }
}