<?php

namespace {
    define('CONST_FOO', 'CONST_FOO');
}

namespace Foo {
    class Bar
    {
        const FOO = 'foo';
    }
}

namespace ServiceRef {
    interface FooServiceInterface
    {
    }

    class FooService implements FooServiceInterface
    {
        /** @var BarService */
        public $bar;

        public function __construct(BarService $bar)
        {
        }

        public function setBar(BarService $bar): void
        {
        }

        public function withBar(BarService $bar): FooService
        {
        }
    }

    class BarService
    {
    }

    class FooServiceFactory
    {
        public function create(): FooServiceInterface
        {
        }

        public function __invoke(): FooServiceInterface
        {
        }
    }

    class FooServiceDecorator
    {
        public function __construct(FooServiceInterface $fooService)
        {
        }
    }

    class FooServiceConfigurator
    {
        public function configure(FooServiceInterface $fooService)
        {

        }

        public function __invoke(): FooServiceInterface
        {
        }
    }

}