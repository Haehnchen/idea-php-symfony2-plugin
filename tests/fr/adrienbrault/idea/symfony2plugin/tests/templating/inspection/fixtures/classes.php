<?php

namespace Foo
{
    class Bar
    {
        const FOO = null;
        private $private;
        public $public;

        public function getFoo() {}
        public function isCar() {}
        protected function getApple() {}

        public function getNext() {
            return new Bar();
        }

        public function getArray() {
            return ['foo' => 'foobar'];
        }
    }

    abstract class BarArrayAccess implements \ArrayAccess
    {
    }
    abstract class BarIterator implements \Iterator
    {
    }

    abstract class BarArrayIterator extends \ArrayIterator
    {
    }
}