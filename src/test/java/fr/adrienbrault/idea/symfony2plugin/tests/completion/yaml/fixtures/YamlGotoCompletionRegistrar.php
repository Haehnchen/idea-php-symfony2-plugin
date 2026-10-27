<?php

namespace Foo
{
    class Bar
    {
        public const BAZ = 'baz';
        private const PRIVATE_CONST = 'private';
        protected const PROTECTED_CONST = 'protected';

        public function create() {}
    }

    class Foobar extends Bar
    {
        public function __construct($arg = null) {}
    }

    enum Suit: string
    {
        case Hearts = 'H';
        case Spades = 'S';

        const WILD = self::Spades;
    }
}
