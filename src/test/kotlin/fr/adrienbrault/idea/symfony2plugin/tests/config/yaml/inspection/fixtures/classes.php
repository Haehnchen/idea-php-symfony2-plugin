<?php

namespace Foo\Service\Method
{
    class MyFoo
    {
        public function getFoo() {}
    }
}

namespace Symfony\Component\EventDispatcher
{
    interface EventSubscriberInterface {}
}

namespace Symfony\Component\EventDispatcher\Attribute
{
    #[\Attribute(\Attribute::TARGET_CLASS | \Attribute::TARGET_METHOD | \Attribute::IS_REPEATABLE)]
    class AsEventListener
    {
        public function __construct(
            public ?string $event = null,
            public ?string $method = null,
            public int $priority = 0,
            public ?string $dispatcher = null,
        ) {
        }
    }
}
